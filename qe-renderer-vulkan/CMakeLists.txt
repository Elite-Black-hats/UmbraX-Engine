cmake_minimum_required(VERSION 3.22.1)

project("vulkan_renderer" CXX)

# C++ 17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Optimización
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wno-unused-parameter -ffast-math")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -O0")

# Encontrar librerías Android
find_library(VULKAN_LIB vulkan REQUIRED)
find_library(ANDROID_LIB android REQUIRED)
find_library(LOG_LIB log REQUIRED)

# Archivos fuente
set(NATIVE_SRCS
    src/main/cpp/vulkan_jni.cpp
    src/main/cpp/vulkan_renderer_native.cpp
    src/main/cpp/vk_instance.cpp
    src/main/cpp/vk_device.cpp
    src/main/cpp/vk_swapchain.cpp
    src/main/cpp/vk_pipeline.cpp
    src/main/cpp/vk_buffer.cpp
    src/main/cpp/vk_texture.cpp
    src/main/cpp/vk_shader.cpp
    src/main/cpp/vk_renderer.cpp
    src/main/cpp/vk_mesh.cpp
    src/main/cpp/vk_material.cpp
    src/main/cpp/vk_compute.cpp
    src/main/cpp/vk_utils.cpp
)

# Crear librería compartida
add_library(vulkan_renderer SHARED ${NATIVE_SRCS})

# Includes
target_include_directories(vulkan_renderer PRIVATE src/main/cpp/include)

# Link
target_link_libraries(
    vulkan_renderer
    ${VULKAN_LIB}
    ${ANDROID_LIB}
    ${LOG_LIB}
)

# Defines
target_compile_definitions(
    vulkan_renderer 
    PRIVATE 
    VK_USE_PLATFORM_ANDROID_KHR
    VK_NO_PROTOTYPES=0
)
