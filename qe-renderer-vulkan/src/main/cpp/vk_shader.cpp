#include "vulkan_renderer_native.h"
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "VulkanShader", __VA_ARGS__)

uint64_t VulkanRendererNative::compileShader(const uint8_t* spirvCode, int codeSize, int stage) {
    LOGI("Compiling shader");
    
    VkShaderModuleCreateInfo createInfo{};
    createInfo.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    createInfo.codeSize = codeSize;
    createInfo.pCode = reinterpret_cast<const uint32_t*>(spirvCode);
    
    auto shader = std::make_shared<Shader>();
    
    if (vkCreateShaderModule(device, &createInfo, nullptr, &shader->module) != VK_SUCCESS) {
        LOGE("Failed to create shader module");
        return 0;
    }
    
    shader->stage = static_cast<VkShaderStageFlagBits>(stage);
    
    uint64_t handle = nextResourceId++;
    shaders[handle] = shader;
    
    return handle;
}
