#include "vulkan_renderer_native.h"
#include <android/log.h>
#include <vector>
#include <set>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "VulkanDevice", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "VulkanDevice", __VA_ARGS__)

bool VulkanRendererNative::pickPhysicalDevice() {
    LOGI("Picking physical device");
    
    uint32_t deviceCount = 0;
    vkEnumeratePhysicalDevices(instance, &deviceCount, nullptr);
    
    if (deviceCount == 0) {
        LOGE("Failed to find GPUs with Vulkan support");
        return false;
    }
    
    std::vector<VkPhysicalDevice> devices(deviceCount);
    vkEnumeratePhysicalDevices(instance, &deviceCount, devices.data());
    
    // Seleccionar el primer dispositivo adecuado
    for (const auto& device : devices) {
        VkPhysicalDeviceProperties properties;
        vkGetPhysicalDeviceProperties(device, &properties);
        
        LOGI("Found device: %s", properties.deviceName);
        
        physicalDevice = device;
        deviceProperties = properties;
        vkGetPhysicalDeviceFeatures(physicalDevice, &deviceFeatures);
        
        // Verificar soporte de ray tracing
        VkPhysicalDeviceRayTracingPipelinePropertiesKHR rtProps{};
        rtProps.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_PIPELINE_PROPERTIES_KHR;
        
        VkPhysicalDeviceProperties2 props2{};
        props2.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2;
        props2.pNext = &rtProps;
        
        vkGetPhysicalDeviceProperties2(physicalDevice, &props2);
        
        return true;
    }
    
    LOGE("Failed to find suitable GPU");
    return false;
}

bool VulkanRendererNative::createLogicalDevice() {
    LOGI("Creating logical device");
    
    // Encontrar queue families
    uint32_t queueFamilyCount = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, &queueFamilyCount, nullptr);
    
    std::vector<VkQueueFamilyProperties> queueFamilies(queueFamilyCount);
    vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, &queueFamilyCount, queueFamilies.data());
    
    // Encontrar graphics queue family
    graphicsQueueFamily = UINT32_MAX;
    computeQueueFamily = UINT32_MAX;
    transferQueueFamily = UINT32_MAX;
    
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
        LOGE("Failed to find graphics queue family");
        return false;
    }
    
    // Crear queues
    std::vector<VkDeviceQueueCreateInfo> queueCreateInfos;
    std::set<uint32_t> uniqueQueueFamilies = {
        graphicsQueueFamily,
        computeQueueFamily != UINT32_MAX ? computeQueueFamily : graphicsQueueFamily,
        transferQueueFamily != UINT32_MAX ? transferQueueFamily : graphicsQueueFamily
    };
    
    float queuePriority = 1.0f;
    for (uint32_t queueFamily : uniqueQueueFamilies) {
        VkDeviceQueueCreateInfo queueCreateInfo{};
        queueCreateInfo.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
        queueCreateInfo.queueFamilyIndex = queueFamily;
        queueCreateInfo.queueCount = 1;
        queueCreateInfo.pQueuePriorities = &queuePriority;
        queueCreateInfos.push_back(queueCreateInfo);
    }
    
    // Device extensions
    std::vector<const char*> deviceExtensions = {
        VK_KHR_SWAPCHAIN_EXTENSION_NAME
    };
    
    // Device features
    VkPhysicalDeviceFeatures features{};
    features.samplerAnisotropy = VK_TRUE;
    features.fillModeNonSolid = VK_TRUE;
    
    // Device create info
    VkDeviceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    createInfo.queueCreateInfoCount = static_cast<uint32_t>(queueCreateInfos.size());
    createInfo.pQueueCreateInfos = queueCreateInfos.data();
    createInfo.pEnabledFeatures = &features;
    createInfo.enabledExtensionCount = static_cast<uint32_t>(deviceExtensions.size());
    createInfo.ppEnabledExtensionNames = deviceExtensions.data();
    
    // Crear device
    if (vkCreateDevice(physicalDevice, &createInfo, nullptr, &device) != VK_SUCCESS) {
        LOGE("Failed to create logical device");
        return false;
    }
    
    // Obtener queues
    vkGetDeviceQueue(device, graphicsQueueFamily, 0, &graphicsQueue);
    vkGetDeviceQueue(device, computeQueueFamily != UINT32_MAX ? computeQueueFamily : graphicsQueueFamily, 0, &computeQueue);
    vkGetDeviceQueue(device, transferQueueFamily != UINT32_MAX ? transferQueueFamily : graphicsQueueFamily, 0, &transferQueue);
    
    LOGI("Logical device created successfully");
    return true;
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
    
    LOGE("Failed to find suitable memory type");
    return 0;
}
