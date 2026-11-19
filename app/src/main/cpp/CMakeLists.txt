cmake_minimum_required(VERSION 3.22.1)
project("myhifiplayer")

set(CMAKE_CXX_STANDARD 17)

add_library(myhifiplayer SHARED
    native-lib.cpp
    DSPEngine.cpp
    AudioEngine.cpp
)

find_package(oboe REQUIRED CONFIG)

target_link_libraries(myhifiplayer oboe log android)
