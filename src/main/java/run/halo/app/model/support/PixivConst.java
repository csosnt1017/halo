package run.halo.app.model.support;

/**
 * Copyright © 2019年 erciyuanboot. All rights reserved.
 *
 * @author 古今
 * <p>
 * xxxxx类
 * @date 2019/09/26
 * <p>
 * Modification History:
 * Date     Author    Version      Description
 * ---------------------------------------------------------*
 * 2019/09/26   古今   v1.0.0       新增
 */
public interface PixivConst {
    class Mode {
        public static final String MODE_DAILY = "daily";

        public static final String MODE_WEEKLY = "weekly";

        public static final String MODE_MONTHLY = "monthly";
        /**
         * 原创
         */
        public static final String MODE_ORIGINAL = "original";

        public static final String MODE_MALE = "male";

        public static final String MODE_FEMALE = "female";

        public static final String MODE_R18 ="daily_r18";
    }

    class Content {
        /**
         * 插画
         */
        public static final String CONTENT_ILLUST = "illust";
        /**
         * 动图
         */
        public static final String CONTENT_UGOIRA = "ugoira";
        /**
         * 漫画
         */
        public static final String CONTENT_MANGA = "manga";

    }

    class RankMode{
        public static final String gruop_1 = "p站排行";

        public static final String gruop_2 = "p站今日";

        public static final String gruop_3 = "p站本周";

        public static final String gruop_4 = "p站本月";

        public static final String gruop_5 = "p站原创";

        public static final String gruop_6 = "p站受男性欢迎";

        public static final String gruop_7 = "p站受女性欢迎";

        public static final String gruop_8 = "p站r18";

    }

    class RankContent{
        public static final String gruop_1 = "p站排行";

        public static final String gruop_2 = "综合";

        public static final String gruop_3 = "插画";

        public static final String gruop_4 = "动图";

        public static final String gruop_5 = "漫画";
    }
}