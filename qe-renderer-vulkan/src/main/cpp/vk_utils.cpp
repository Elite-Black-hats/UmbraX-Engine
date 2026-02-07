// vk_utils.cpp
#include "vulkan_renderer_native.h"
#include <android/log.h>
#include <string>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "VulkanUtils", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "VulkanUtils", __VA_ARGS__)

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

VulkanInfo VulkanRendererNative::getVulkanInfo() const {
    VulkanInfo info;
    
    if (physicalDevice == VK_NULL_HANDLE) {
        LOGW("Physical device not initialized");
        return info;
    }
    
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
        case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU:
            info.deviceType = "Virtual GPU";
            break;
        case VK_PHYSICAL_DEVICE_TYPE_CPU:
            info.deviceType = "CPU";
            break;
        default:
            info.deviceType = "Other";
    }
    
    info.maxTextureSize = deviceProperties.limits.maxImageDimension2D;
    info.supportsRayTracing = false; // TODO: Check actual support
    info.supportsMeshShaders = false; // TODO: Check actual support
    
    LOGI("Device: %s, API: %s, Max Texture: %d", 
         info.deviceName.c_str(), 
         info.apiVersion.c_str(), 
         info.maxTextureSize);
    
    return info;
}
