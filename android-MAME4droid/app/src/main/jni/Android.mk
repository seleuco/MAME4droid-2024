# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS    := -ldl -llog
LOCAL_MODULE    := mame4droid-jni
LOCAL_SRC_FILES := mame4droid-jni.c


#traverse all the directory and subdirectory
#define walk
  #$(wildcard $(1)) $(foreach e, $(wildcard $(1)/*), $(call walk, $(e)))
#endef
#LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../../../../src/emu
#LOCAL_C_INCLUDES += $(LOCAL_PATH)/../../../../../src/mame
#ALLFILES = $(call walk, $(LOCAL_PATH)/../../../../../src)
#FILE_LIST := $(filter %.cpp, $(ALLFILES))
#LOCAL_SRC_FILES += $(FILE_LIST:$(LOCAL_PATH)/%=%)


include $(BUILD_SHARED_LIBRARY)

