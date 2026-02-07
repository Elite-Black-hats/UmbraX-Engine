#ifndef VULKAN_RENDERER_NATIVE_H
#define VULKAN_RENDERER_NATIVE_H

#include <vulkan/vulkan.h>
#include <android/native_window.h>
#include <string>
#include <vector>
#include <memory>
#include <unordered_map>

// Estructuras de datos

struct VulkanInfo {
    std::string deviceName;
    std::string apiVersion;
    std::string driverVersion;
    int vendorId;
    std::string deviceType;
    int maxTextureSize;
    bool supportsRayTracing;
    bool supportsMeshShaders;
};

struct Mesh {
    VkBuffer vertexBuffer;
    VkBuffer indexBuffer;
    VkDeviceMemory vertexMemory;
    VkDeviceMemory indexMemory;
    uint32_t indexCount;
};

struct Texture {
    VkImage image;
    VkImageView imageView;
    VkSampler sampler;
    VkDeviceMemory memory;
};

struct Shader {
    VkShaderModule module;
    VkShaderStageFlagBits stage;
};

struct Pipeline {
    VkPipeline pipeline;
    VkPipelineLayout layout;
};

// Clase principal del renderer

class VulkanRendererNative {
public:
    VulkanRendererNative();
    ~VulkanRendererNative();
    
    // Lifecycle
    bool initialize();
    void shutdown();
    
    // Surface
    void setSurface(ANativeWindow* window, int width, int height);
    
    // Frame
    void beginFrame();
    void endFrame();
    
    // Rendering
    void submitMesh(uint64_t meshHandle, const float* transform, const float* color);
    void setViewProjection(const float* view, const float* projection);
    void setClearColor(float r, float g, float b, float a);
    void setViewport(int x, int y, int width, int height);
    
    // Resources
    uint64_t loadMesh(const float* vertices, int vertexCount,
                      const uint32_t* indices, int indexCount,
                      const float* normals, int normalCount,
                      const float* uvs, int uvCount);
    uint64_t loadTexture(const uint8_t* pixels, int width, int height, int format);
    uint64_t compileShader(const uint8_t* spirvCode, int codeSize, int stage);
    uint64_t createGraphicsPipeline(uint64_t vertexShader, uint64_t fragmentShader,
                                     const int* config);
    
    // Compute
    void dispatchCompute(uint64_t computeShader, int groupsX, int groupsY, int groupsZ);
    
    // Ray Tracing
    bool supportsRayTracing() const;
    void traceRays(uint64_t raygenShader, uint64_t missShader, uint64_t hitShader,
                   int width, int height);
    
    // Info
    VulkanInfo getVulkanInfo() const;
    
private:
    // Vulkan objects
    VkInstance instance;
    VkPhysicalDevice physicalDevice;
    VkDevice device;
    VkQueue graphicsQueue;
    VkQueue computeQueue;
    VkQueue transferQueue;
    
    VkSurfaceKHR surface;
    VkSwapchainKHR swapchain;
    std::vector<VkImage> swapchainImages;
    std::vector<VkImageView> swapchainImageViews;
    std::vector<VkFramebuffer> framebuffers;
    
    VkRenderPass renderPass;
    VkCommandPool commandPool;
    VkCommandPool computeCommandPool;
    std::vector<VkCommandBuffer> commandBuffers;
    
    VkSemaphore imageAvailableSemaphore;
    VkSemaphore renderFinishedSemaphore;
    VkFence inFlightFence;
    
    VkDescriptorPool descriptorPool;
    VkDescriptorSetLayout descriptorSetLayout;
    
    // Properties
    VkPhysicalDeviceProperties deviceProperties;
    VkPhysicalDeviceFeatures deviceFeatures;
    VkPhysicalDeviceRayTracingPipelineFeaturesKHR rtFeatures;
    VkPhysicalDeviceMeshShaderFeaturesEXT meshShaderFeatures;
    
    uint32_t graphicsQueueFamily;
    uint32_t computeQueueFamily;
    uint32_t transferQueueFamily;
    
    uint32_t currentFrame;
    uint32_t imageIndex;
    
    VkExtent2D swapchainExtent;
    VkFormat swapchainFormat;
    
    VkClearColorValue clearColor;
    
    // Resource management
    std::unordered_map<uint64_t, std::shared_ptr<Mesh>> meshes;
    std::unordered_map<uint64_t, std::shared_ptr<Texture>> textures;
    std::unordered_map<uint64_t, std::shared_ptr<Shader>> shaders;
    std::unordered_map<uint64_t, std::shared_ptr<Pipeline>> pipelines;
    
    uint64_t nextResourceId;
    
    // Helper functions
    bool createInstance();
    bool pickPhysicalDevice();
    bool createLogicalDevice();
    bool createSwapchain();
    bool createRenderPass();
    bool createFramebuffers();
    bool createCommandPools();
    bool createCommandBuffers();
    bool createSyncObjects();
    bool createDescriptorPool();
    
    void destroySwapchain();
    void recreateSwapchain();
    
    uint32_t findMemoryType(uint32_t typeFilter, VkMemoryPropertyFlags properties);
    void createBuffer(VkDeviceSize size, VkBufferUsageFlags usage,
                     VkMemoryPropertyFlags properties,
                     VkBuffer& buffer, VkDeviceMemory& memory);
    void copyBuffer(VkBuffer srcBuffer, VkBuffer dstBuffer, VkDeviceSize size);
};

#endif // VULKAN_RENDERER_NATIVE_H
