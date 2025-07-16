#!/bin/bash

# 🚀 ملف تشغيل تطبيق الدردشة المشفرة
# Advanced Encrypted Bluetooth Chat Application

echo "🔐 بدء تشغيل تطبيق الدردشة المشفرة المتقدم..."
echo "=================================================="

# التحقق من وجود Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "❌ خطأ: ANDROID_HOME غير محدد"
    echo "يرجى تثبيت Android SDK وتحديد متغير ANDROID_HOME"
    exit 1
fi

# التحقق من وجود ADB
if ! command -v adb &> /dev/null; then
    echo "❌ خطأ: ADB غير مثبت"
    echo "يرجى تثبيت Android SDK tools"
    exit 1
fi

# التحقق من اتصال الجهاز
echo "🔍 التحقق من اتصال الجهاز..."
if ! adb devices | grep -q "device$"; then
    echo "❌ لا يوجد جهاز متصل"
    echo "يرجى توصيل جهاز Android وتفعيل وضع المطور"
    echo "أو تشغيل محاكي Android"
    exit 1
fi

echo "✅ تم العثور على جهاز متصل"

# تنظيف المشروع
echo "🧹 تنظيف المشروع..."
if [ -f "gradlew" ]; then
    ./gradlew clean
else
    echo "❌ ملف gradlew غير موجود"
    echo "يرجى تشغيل الأمر من مجلد المشروع الرئيسي"
    exit 1
fi

# بناء التطبيق
echo "🔨 بناء التطبيق..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ تم بناء التطبيق بنجاح"
else
    echo "❌ فشل في بناء التطبيق"
    exit 1
fi

# تثبيت التطبيق
echo "📱 تثبيت التطبيق على الجهاز..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo "✅ تم تثبيت التطبيق بنجاح"
else
    echo "❌ فشل في تثبيت التطبيق"
    exit 1
fi

# تشغيل التطبيق
echo "🚀 تشغيل التطبيق..."
adb shell am start -n com.bluetoothchat.encrypted/.ui.advanced.AdvancedMainActivity

echo "=================================================="
echo "🎉 تم تشغيل التطبيق بنجاح!"
echo ""
echo "📋 الخطوات التالية:"
echo "1. امنح الصلاحيات المطلوبة (Bluetooth, الموقع, الميكروفون)"
echo "2. فعل Bluetooth في إعدادات الجهاز"
echo "3. اضغط زر البحث لاكتشاف الأجهزة"
echo "4. اختر جهاز واضغط 'اتصال'"
echo "5. ابدأ المحادثة!"
echo ""
echo "🔒 ميزات التطبيق:"
echo "• تشفير AES-256-GCM متقدم"
echo "• ذكاء اصطناعي لتحليل المشاعر"
echo "• شبكة mesh للاتصال المتعدد"
echo "• صوت عالي الجودة مع إلغاء الضوضاء"
echo "• واجهة مستخدم حديثة بـ Material Design 3"
echo ""
echo "📖 للمزيد من المعلومات:"
echo "• دليل التشغيل السريع: QUICK_START.md"
echo "• دليل التشغيل الشامل: MOBILE_SETUP.md"
echo "• التوثيق الكامل: README.md"
echo ""
echo "🎯 استمتع بتجربة دردشة آمنة ومشفرة!"