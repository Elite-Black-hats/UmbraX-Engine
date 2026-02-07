#include "vulkan_renderer_native.h"
#include <android/log.h>
#include <vector>
#include <cstring>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "VulkanInstance", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "VulkanInstance", __VA_ARGS__)

bool VulkanRendererNative::createInstance() {
    LOGI("Creating Vulkan instance");
    
    // Application info
    VkApplicationInfo appInfo{};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.pApplicationName = "Quantum Engine";
    appInfo.applicationVersion = VK_MAKE_VERSION(3, 0, 0);
    appInfo.pEngineName = "Quantum";
    appInfo.engineVersion = VK_MAKE_VERSION(3, 0, 0);
    appInfo.apiVersion = VK_API_VERSION_1_3;
    
    // Extensions requeridas
    std::vector<const char*> extensions = {
        VK_KHR_SURFACE_EXTENSION_NAME,
        VK_KHR_ANDROID_SURFACE_EXTENSION_NAME
    };
    
    // Instance create info
    VkInstanceCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    createInfo.enabledExtensionCount = static_cast<uint32_t>(extensions.size());
    createInfo.ppEnabledExtensionNames = extensions.data();
    createInfo.enabledLayerCount = 0;
    
    // Crear instance
    VkResult result = vkCreateInstance(&createInfo, nullptr, &instance);
    
    if (result != VK_SUCCESS) {
        LOGE("Failed to create Vulkan instance: %d", result);
        return false;
    }
    
    LOGI("Vulkan instance created successfully");
    return true;
}
