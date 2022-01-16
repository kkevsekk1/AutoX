package com.baidu.paddle.lite.demo.ocr;

public class OcrConfig {
    public static final String ASSETS_MODEL_DIR_PATH = "models/ocr_v2_for_cpu";
    public static final String ASSETS_LABEL_FILE_PATH = "labels/ppocr_keys_v1.txt";
    public static final int DEFAULT_CPU_THREAD = 4;
    public static final String DEFAULT_CPU_RUN_MODE = "LITE_POWER_HIGH";
    public static final String DET_MODEL_NAME = "ch_ppocr_mobile_v2.0_det_opt.nb";
    public static final String REC_MODEL_NAME = "ch_ppocr_mobile_v2.0_rec_opt.nb";
    public static final String CLS_MODEL_NAME = "ch_ppocr_mobile_v2.0_cls_opt.nb";
    public static final int IMG_DATA_CHANNELS = 3;
    public static final String IMG_INPUT_COLOR_FORMAT = "BGR";
    public static final int[] CHANNEL_IDX = new int[]{2, 1, 0};
    public static final int IMG_DATA_MAC_SIZE = 960;
    public static final long[] INPUT_SHAPE = new long[]{1L, 3L, 960L};
    public static final float[] INPUT_MEAN = new float[]{0.485F, 0.456F, 0.406F};
    public static final float[] INPUT_STD = new float[]{0.229F, 0.224F, 0.225F};

    public OcrConfig() {
    }
}
