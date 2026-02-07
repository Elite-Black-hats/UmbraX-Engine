#include "vulkan_renderer_native.h"
#include <android/log.h>
#include <cstring>
#include <algorithm>

#define LOG_TAG "VulkanRendererNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

VulkanRendererNative::VulkanRendererNative()
    : instance(VK_NULL_HANDLE)
    , physicalDevice(VK_NULL_HANDLE)
    , device(VK_NULL_HANDLE)
    , graphicsQueue(VK_NULL_HANDLE)
    , computeQueue(VK_NULL_HANDLE)
    , transferQueue(VK_NULL_HANDLE)
    , surface(VK_NULL_HANDLE)
    , swapchain(VK_NULL_HANDLE)
    , renderPass(VK_NULL_HANDLE)
    , commandPool(VK_NULL_HANDLE)
    , computeCommandPool(VK_NULL_HANDLE)
    , imageAvailableSemaphore(VK_NULL_HANDLE)
    , renderFinishedSemaphore(VK_NULL_HANDLE)
    , inFlightFence(VK_NULL_HANDLE)
    , descriptorPool(VK_NULL_HANDLE)
    , descriptorSetLayout(VK_NULL_HANDLE)
    , graphicsQueueFamily(UINT32_MAX)
    , computeQueueFamily(UINT32_MAX)
    , transferQueueFamily(UINT32_MAX)
    , currentFrame(0)
    , imageIndex(0)
    , nextResourceId(1)
{
    clearColor = {{0.1f, 0.1f, 0.15f, 1.0f}};
    swapchainExtent = {0, 0};
    swapchainFormat = VK_FORMAT_UNDEFINED;
}

VulkanRendererNative::~VulkanRendererNative() {
    shutdown();
}

bool VulkanRendererNative::initialize() {
    LOGI("Initializing Vulkan Renderer");
    
    if (!createInstance()) {
        LOGE("Failed to create Vulkan instance");
        return false;
    }
    
    if (!pickPhysicalDevice()) {
        LOGE("Failed to pick physical device");
        return false;
    }
    
    if (!createLogicalDevice()) {
        LOGE("Failed to create logical device");
        return false;
    }
    
    if (!createCommandPools()) {
        LOGE("Failed to create command pools");
        return false;
    }
    
    if (!createSyncObjects()) {
        LOGE("Failed to create sync objects");
        return false;
    }
    
    if (!createDescriptorPool()) {
        LOGE("Failed to create descriptor pool");
        return false;
    }
    
    LOGI("Vulkan Renderer initialized successfully");
    return true;
}

void VulkanRendererNative::shutdown() {
    if (device != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(device);
        
        // Cleanup resources
        meshes.clear();
        textures.clear();
        shaders.clear();
        pipelines.clear();
        
        // Destroy sync objects
        if (inFlightFence != VK_NULL_HANDLE) {
            vkDestroyFence(device, inFlightFence, nullptr);
            inFlightFence = VK_NULL_HANDLE;
        }
        if (renderFinishedSemaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(device, renderFinishedSemaphore, nullptr);
            renderFinishedSemaphore = VK_NULL_HANDLE;
        }
        if (imageAvailableSemaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(device, imageAvailableSemaphore, nullptr);
            imageAvailableSemaphore = VK_NULL_HANDLE;
        }
        
        destroySwapchain();
        
        if (descriptorPool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(device, descriptorPool, nullptr);
            descriptorPool = VK_NULL_HANDLE;
        }
        
        if (commandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(device, commandPool, nullptr);
            commandPool = VK_NULL_HANDLE;
        }
        
        if (computeCommandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(device, computeCommandPool, nullptr);
            computeCommandPool = VK_NULL_HANDLE;
        }
        
        vkDestroyDevice(device, nullptr);
        device = VK_NULL_HANDLE;
    }
    
    if (surface != VK_NULL_HANDLE) {
        vkDestroySurfaceKHR(instance, surface, nullptr);
        surface = VK_NULL_HANDLE;
    }
    
    if (instance != VK_NULL_HANDLE) {
        vkDestroyInstance(instance, nullptr);
        instance = VK_NULL_HANDLE;
    }
}

bool VulkanRendererNative::createInstance() {
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "Quantum Engine";
    appInfo.applicationVersion = VK_MAKE_VERSION(3, 0, 0);
    appInfo.pEngineName = "Quantum";
    appInfo.engineVersion = VK_MAKE_VERSION(3, 0, 0);
    appInfo.apiVersion = VK_API_VERSION_1_3;
    
    std::vector<const char*> extensions = {
        VK_KHR_SURFACE_EXTENSION_NAME,
        VK_KHR_ANDROID_SURFACE_EXTENSION_NAME
    };
    
    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    createInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    createInfo.ppEnabledExtensionNames = extensions.data();
    createInfo.enabledLayerCount = 0;
    
    VkResult result = vkCreateInstance(&createInfo, nullptr, &instance);
    if (result != VK_SUCCESS) {
        LOGE("vkCreateInstance failed: %d", result);
        return false;
    }
    
    return true;
}

bool VulkanRendererNative::pickPhysicalDevice() {
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    
    if (deviceCount == 0) {
        LOGE("No Vulkan devices found");
        return false;
    }
    
    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());
    
    // Pick first suitable device
    for (const auto& dev : devices) {
        vkGetPhysicalDeviceProperties(dev, &deviceProperties);
        vkGetPhysicalDeviceFeatures(dev, &deviceFeatures);
        
        // Check if device is suitable
        physicalDevice = dev;
        LOGI("Selected device: %s", deviceProperties.deviceName);
        return true;
    }
    
    LOGE("No suitable device found");
    return false;
}

bool VulkanRendererNative::createLogicalDevice() {
    // Find queue families
    uint32_t queueFamilyCount = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, &queueFamilyCount, nullptr);
    
    std::vector<VkQueueFamilyProperties> queueFamilies(queueFamilyCount);
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, &queueFamilyCount, queueFamilies.data());
    
    for (uint32_t i = 0; i < queueFamilyCount; i++) {
        if (queueFamilies[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) {
            graphicsQueueFamily = i;
        }
        if (queueFamilies[i].queueFlags & VK_QUEUE_COMPUTE_BIT) {
            computeQueueFamily = i;
        }
        if (queueFamilies[i].queueFlags & VK_QUEUE_TRANSFER_BIT) {
            transferQueueFamily = i;
        }
    }
    
    if (graphicsQueueFamily == UINT32_MAX) {
        LOGE("No graphics queue family found");
        return false;
    }
    
    // Use graphics queue for compute/transfer if dedicated queues not found
    if (computeQueueFamily == UINT32_MAX) computeQueueFamily = graphicsQueueFamily;
    if (transferQueueFamily == UINT32_MAX) transferQueueFamily = graphicsQueueFamily;
    
    std::vector<VkDeviceQueueCreateInfo> queueCreateInfos;
    float queuePriority = 1.0f;
    
    VkDeviceQueueCreateInfo queueCreateInfo{};
    queueCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    queueCreateInfo.queueFamilyIndex = graphicsQueueFamily;
    queueCreateInfo.queueCount = 1;
    queueCreateInfo.pQueuePriorities = &queuePriority;
    queueCreateInfos.push_back(queueCreateInfo);
    
    std::vector<const char*> deviceExtensions = {
        VK_KHR_SWAPCHAIN_EXTENSION_NAME
    };
    
    VkPhysicalDeviceFeatures enabledFeatures{};
    
    VkDeviceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    createInfo.queueCreateInfoCount = static_cast<uint32_t>(queueCreateInfos.size());
    createInfo.pQueueCreateInfos = queueCreateInfos.data();
    createInfo.pEnabledFeatures = &enabledFeatures;
    createInfo.enabledExtensionCount = static_cast<uint32_t>(deviceExtensions.size());
    createInfo.ppEnabledExtensionNames = deviceExtensions.data();
    
    if (vkCreateDevice(physicalDevice, &createInfo, nullptr, &device) != VK_SUCCESS) {
        LOGE("Failed to create logical device");
        return false;
    }
    
    vkGetDeviceQueue(device, graphicsQueueFamily, 0, &graphicsQueue);
    computeQueue = graphicsQueue;
    transferQueue = graphicsQueue;
    
    return true;
}

bool VulkanRendererNative::createCommandPools() {
    VkCommandPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    poolInfo.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    poolInfo.queueFamilyIndex = graphicsQueueFamily;
    
    if (vkCreateCommandPool(device, &poolInfo, nullptr, &commandPool) != VK_SUCCESS) {
        return false;
    }
    
    poolInfo.queueFamilyIndex = computeQueueFamily;
    if (vkCreateCommandPool(device, &poolInfo, nullptr, &computeCommandPool) != VK_SUCCESS) {
        return false;
    }
    
    return true;
}

bool VulkanRendererNative::createSyncObjects() {
    VkSemaphoreCreateInfo semaphoreInfo{};
    semaphoreInfo.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
    
    VkFenceCreateInfo fenceInfo{};
    fenceInfo.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    fenceInfo.flags = VK_FENCE_CREATE_SIGNALED_BIT;
    
    if (vkCreateSemaphore(device, &semaphoreInfo, nullptr, &imageAvailableSemaphore) != VK_SUCCESS ||
        vkCreateSemaphore(device, &semaphoreInfo, nullptr, &renderFinishedSemaphore) != VK_SUCCESS ||
        vkCreateFence(device, &fenceInfo, nullptr, &inFlightFence) != VK_SUCCESS) {
        return false;
    }
    
    return true;
}

bool VulkanRendererNative::createDescriptorPool() {
    std::vector<VkDescriptorPoolSize> poolSizes = {
        {VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 100},
        {VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 100}
    };
    
    VkDescriptorPoolCreateInfo poolInfo{};
    poolInfo.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    poolInfo.poolSizeCount = static_cast<uint32_t>(poolSizes.size());
    poolInfo.pPoolSizes = poolSizes.data();
    poolInfo.maxSets = 100;
    
    return vkCreateDescriptorPool(device, &poolInfo, nullptr, &descriptorPool) == VK_SUCCESS;
}

void VulkanRendererNative::setSurface(ANativeWindow* window, int width, int height) {
    LOGI("Setting surface: %dx%d", width, height);
    
    VkAndroidSurfaceCreateInfoKHR surfaceCreateInfo{};
    surfaceCreateInfo.sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
    surfaceCreateInfo.window = window;
    
    if (vkCreateAndroidSurfaceKHR(instance, &surfaceCreateInfo, nullptr, &surface) != VK_SUCCESS) {
        LOGE("Failed to create Android surface");
        return;
    }
    
    swapchainExtent.width = width;
    swapchainExtent.height = height;
    
    createSwapchain();
    createRenderPass();
    createFramebuffers();
    createCommandBuffers();
}

bool VulkanRendererNative::createSwapchain() {
    swapchainFormat = VK_FORMAT_B8G8R8A8_UNORM;
    
    VkSwapchainCreateInfoKHR createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
    createInfo.surface = surface;
    createInfo.minImageCount = 2;
    createInfo.imageFormat = swapchainFormat;
    createInfo.imageColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    createInfo.imageExtent = swapchainExtent;
    createInfo.imageArrayLayers = 1;
    createInfo.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    createInfo.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    createInfo.preTransform = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
    createInfo.compositeAlpha = VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR;
    createInfo.presentMode = VK_PRESENT_MODE_FIFO_KHR;
    createInfo.clipped = VK_TRUE;
    
    if (vkCreateSwapchainKHR(device, &createInfo, nullptr, &swapchain) != VK_SUCCESS) {
        return false;
    }
    
    uint32_t imageCount;
    vkGetSwapchainImagesKHR(device, swapchain, &imageCount, nullptr);
    swapchainImages.resize(imageCount);
    vkGetSwapchainImagesKHR(device, swapchain, &imageCount, swapchainImages.data());
    
    swapchainImageViews.resize(imageCount);
    for (uint32_t i = 0; i < imageCount; i++) {
        VkImageViewCreateInfo viewInfo{};
        viewInfo.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
        viewInfo.image = swapchainImages[i];
        viewInfo.viewType = VK_IMAGE_VIEW_TYPE_2D;
        viewInfo.format = swapchainFormat;
        viewInfo.components.r = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.components.g = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.components.b = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.components.a = VK_COMPONENT_SWIZZLE_IDENTITY;
        viewInfo.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        viewInfo.subresourceRange.baseMipLevel = 0;
        viewInfo.subresourceRange.levelCount = 1;
        viewInfo.subresourceRange.baseArrayLayer = 0;
        viewInfo.subresourceRange.layerCount = 1;
        
        vkCreateImageView(device, &viewInfo, nullptr, &swapchainImageViews[i]);
    }
    
    return true;
}

bool VulkanRendererNative::createRenderPass() {
    VkAttachmentDescription colorAttachment{};
    colorAttachment.format = swapchainFormat;
    colorAttachment.samples = VK_SAMPLE_COUNT_1_BIT;
    colorAttachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    colorAttachment.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    colorAttachment.stencilLoadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE;
    colorAttachment.stencilStoreOp = VK_ATTACHMENT_STORE_OP_DONT_CARE;
    colorAttachment.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    colorAttachment.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    
    VkAttachmentReference colorAttachmentRef{};
    colorAttachmentRef.attachment = 0;
    colorAttachmentRef.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
    
    VkSubpassDescription subpass{};
    subpass.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    subpass.colorAttachmentCount = 1;
    subpass.pColorAttachments = &colorAttachmentRef;
    
    VkRenderPassCreateInfo renderPassInfo{};
    renderPassInfo.sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
    renderPassInfo.attachmentCount = 1;
    renderPassInfo.pAttachments = &colorAttachment;
    renderPassInfo.subpassCount = 1;
    renderPassInfo.pSubpasses = &subpass;
    
    return vkCreateRenderPass(device, &renderPassInfo, nullptr, &renderPass) == VK_SUCCESS;
}

bool VulkanRendererNative::createFramebuffers() {
    framebuffers.resize(swapchainImageViews.size());
    
    for (size_t i = 0; i < swapchainImageViews.size(); i++) {
        VkImageView attachments[] = {swapchainImageViews[i]};
        
        VkFramebufferCreateInfo framebufferInfo{};
        framebufferInfo.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        framebufferInfo.renderPass = renderPass;
        framebufferInfo.attachmentCount = 1;
        framebufferInfo.pAttachments = attachments;
        framebufferInfo.width = swapchainExtent.width;
        framebufferInfo.height = swapchainExtent.height;
        framebufferInfo.layers = 1;
        
        if (vkCreateFramebuffer(device, &framebufferInfo, nullptr, &framebuffers[i]) != VK_SUCCESS) {
            return false;
        }
    }
    
    return true;
}

bool VulkanRendererNative::createCommandBuffers() {
    commandBuffers.resize(framebuffers.size());
    
    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.commandPool = commandPool;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandBufferCount = static_cast<uint32_t>(commandBuffers.size());
    
    return vkAllocateCommandBuffers(device, &allocInfo, commandBuffers.data()) == VK_SUCCESS;
}

void VulkanRendererNative::destroySwapchain() {
    for (auto framebuffer : framebuffers) {
        vkDestroyFramebuffer(device, framebuffer, nullptr);
    }
    framebuffers.clear();
    
    for (auto imageView : swapchainImageViews) {
        vkDestroyImageView(device, imageView, nullptr);
    }
    swapchainImageViews.clear();
    
    if (renderPass != VK_NULL_HANDLE) {
        vkDestroyRenderPass(device, renderPass, nullptr);
        renderPass = VK_NULL_HANDLE;
    }
    
    if (swapchain != VK_NULL_HANDLE) {
        vkDestroySwapchainKHR(device, swapchain, nullptr);
        swapchain = VK_NULL_HANDLE;
    }
}

void VulkanRendererNative::beginFrame() {
    vkWaitForFences(device, 1, &inFlightFence, VK_TRUE, UINT64_MAX);
    vkResetFences(device, 1, &inFlightFence);
    
    vkAcquireNextImageKHR(device, swapchain, UINT64_MAX, imageAvailableSemaphore, VK_NULL_HANDLE, &imageIndex);
}

void VulkanRendererNative::endFrame() {
    VkPresentInfoKHR presentInfo{};
    presentInfo.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
    presentInfo.waitSemaphoreCount = 1;
    presentInfo.pWaitSemaphores = &renderFinishedSemaphore;
    presentInfo.swapchainCount = 1;
    presentInfo.pSwapchains = &swapchain;
    presentInfo.pImageIndices = &imageIndex;
    
    vkQueuePresentKHR(graphicsQueue, &presentInfo);
}

// Stubs for remaining functions
void VulkanRendererNative::submitMesh(uint64_t meshHandle, const float* transform, const float* color) {}
void VulkanRendererNative::setViewProjection(const float* view, const float* projection) {}
void VulkanRendererNative::setClearColor(float r, float g, float b, float a) {
    clearColor.float32[0] = r;
    clearColor.float32[1] = g;
    clearColor.float32[2] = b;
    clearColor.float32[3] = a;
}
void VulkanRendererNative::setViewport(int x, int y, int width, int height) {}

uint64_t VulkanRendererNative::loadMesh(const float* vertices, int vertexCount,
                                         const uint32_t* indices, int indexCount,
                                         const float* normals, int normalCount,
                                         const float* uvs, int uvCount) {
    return nextResourceId++;
}

uint64_t VulkanRendererNative::loadTexture(const uint8_t* pixels, int width, int height, int format) {
    return nextResourceId++;
}

uint64_t VulkanRendererNative::compileShader(const uint8_t* spirvCode, int codeSize, int stage) {
    return nextResourceId++;
}

uint64_t VulkanRendererNative::createGraphicsPipeline(uint64_t vertexShader, uint64_t fragmentShader,
                                                       const int* config) {
    return nextResourceId++;
}

void VulkanRendererNative::dispatchCompute(uint64_t computeShader, int groupsX, int groupsY, int groupsZ) {}

bool VulkanRendererNative::supportsRayTracing() const {
    return false;
}

void VulkanRendererNative::traceRays(uint64_t raygenShader, uint64_t missShader, uint64_t hitShader,
                                      int width, int height) {}

VulkanInfo VulkanRendererNative::getVulkanInfo() const {
    VulkanInfo info;
    info.deviceName = deviceProperties.deviceName;
    
    uint32_t apiVer = deviceProperties.apiVersion;
    info.apiVersion = std::to_string(VK_VERSION_MAJOR(apiVer)) + "." +
                      std::to_string(VK_VERSION_MINOR(apiVer)) + "." +
                      std::to_string(VK_VERSION_PATCH(apiVer));
    
    info.driverVersion = std::to_string(deviceProperties.driverVersion);
    info.vendorId = deviceProperties.vendorID;
    
    switch (deviceProperties.deviceType) {
        case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU:
            info.deviceType = "Discrete GPU";
            break;
        case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU:
            info.deviceType = "Integrated GPU";
            break;
        default:
            info.deviceType = "Other";
    }
    
    info.maxTextureSize = deviceProperties.limits.maxImageDimension2D;
    info.supportsRayTracing = false;
    info.supportsMeshShaders = false;
    
    return info;
}

uint32_t VulkanRendererNative::findMemoryType(uint32_t typeFilter, VkMemoryPropertyFlags properties) {
    VkPhysicalDeviceMemoryProperties memProperties;
    vkGetPhysicalDeviceMemoryProperties(physicalDevice, &memProperties);
    
    for (uint32_t i = 0; i < memProperties.memoryTypeCount; i++) {
        if ((typeFilter & (1 << i)) && 
            (memProperties.memoryTypes[i].propertyFlags & properties) == properties) {
            return i;
        }
    }
    
    return 0;
}

void VulkanRendererNative::createBuffer(VkDeviceSize size, VkBufferUsageFlags usage,
                                        VkMemoryPropertyFlags properties,
                                        VkBuffer& buffer, VkDeviceMemory& memory) {
    VkBufferCreateInfo bufferInfo{};
    bufferInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bufferInfo.size = size;
    bufferInfo.usage = usage;
    bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    
    vkCreateBuffer(device, &bufferInfo, nullptr, &buffer);
    
    VkMemoryRequirements memRequirements;
    vkGetBufferMemoryRequirements(device, buffer, &memRequirements);
    
    VkMemoryAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    allocInfo.allocationSize = memRequirements.size;
    allocInfo.memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, properties);
    
    vkAllocateMemory(device, &allocInfo, nullptr, &memory);
    vkBindBufferMemory(device, buffer, memory, 0);
}

void VulkanRendererNative::copyBuffer(VkBuffer srcBuffer, VkBuffer dstBuffer, VkDeviceSize size) {
    VkCommandBufferAllocateInfo allocInfo{};
    allocInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    allocInfo.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    allocInfo.commandPool = commandPool;
    allocInfo.commandBufferCount = 1;
    
    VkCommandBuffer commandBuffer;
    vkAllocateCommandBuffers(device, &allocInfo, &commandBuffer);
    
    VkCommandBufferBeginInfo beginInfo{};
    beginInfo.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    beginInfo.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    
    vkBeginCommandBuffer(commandBuffer, &beginInfo);
    
    VkBufferCopy copyRegion{};
    copyRegion.size = size;
    vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, 1, &copyRegion);
    
    vkEndCommandBuffer(commandBuffer);
    
    VkSubmitInfo submitInfo{};
    submitInfo.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    submitInfo.commandBufferCount = 1;
    submitInfo.pCommandBuffers = &commandBuffer;
    
    vkQueueSubmit(transferQueue, 1, &submitInfo, VK_NULL_HANDLE);
    vkQueueWaitIdle(transferQueue);
    
    vkFreeCommandBuffers(device, commandPool, 1, &commandBuffer);
}

void VulkanRendererNative::recreateSwapchain() {
    vkDeviceWaitIdle(device);
    destroySwapchain();
    createSwapchain();
    createRenderPass();
    createFramebuffers();
    createCommandBuffers();
}
