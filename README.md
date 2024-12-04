# cut_image
简单的传入图片，识别人像操作
用法如下
//bitmap-带人象的图片
//this - context上下文
CutImageUtils.onImageChanged(bitmap, this ) { outputImage, inputImage, time ->
//outputimage-识别后的图片
//inputimage-原图
//time-消耗时间毫秒
}
