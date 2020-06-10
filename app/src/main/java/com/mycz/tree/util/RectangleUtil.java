package com.mycz.tree.util;

import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 木已成舟
 * @date 2020/6/10
 */
public class RectangleUtil {

    /**
     * 根据两点确定一个矩形
     * @param p1
     * @param p2
     * @return 矩形的坐标集合
     */
    public static List<LatLng> vertices(LatLng p1, LatLng p2) {

        double x1 = p1.longitude;
        double y1 = p1.latitude;

        double x2 = p2.longitude;
        double y2 = p2.latitude;

        // 长度
        double length = Math.abs(x1 - x2);
        // 宽度
        double width = Math.abs(y1 - y2);

        // 中点坐标
        double centerX = (x1 + x2) / 2;
        double centerY = (y1 + y2) / 2;

        // 左上角点
        double ltX = centerX - length / 2;
        double ltY = centerY + width / 2;
        LatLng lt = new LatLng(ltY, ltX);

        // 右上角点
        double rtX = centerX + length / 2;
        double rtY = centerY + width / 2;
        LatLng rt = new LatLng(rtY, rtX);

        // 左下角点
        double lbX = centerX - length / 2;
        double lbY = centerY - width / 2;
        LatLng lb = new LatLng(lbY, lbX);

        // 右下角点
        double rbX = centerX + length / 2;
        double rbY = centerY - width / 2;
        LatLng rb = new LatLng(rbY, rbX);

        //构建折线点坐标
        List<LatLng> points = new ArrayList<>();
        points.add(lt);
        points.add(rt);
        points.add(rb);
        points.add(lb);
        points.add(lt);

        return points;
    }

    /**
     * 获得矩形的左上角点
     * @param p1
     * @param p2
     * @return
     */
    public static LatLng leftTopVertex(LatLng p1, LatLng p2) {
        double x1 = p1.longitude;
        double y1 = p1.latitude;

        double x2 = p2.longitude;
        double y2 = p2.latitude;

        // 长度
        double length = Math.abs(x1 - x2);
        // 宽度
        double width = Math.abs(y1 - y2);

        // 中点坐标
        double centerX = (x1 + x2) / 2;
        double centerY = (y1 + y2) / 2;

        // 左上角点
        double ltX = centerX - length / 2;
        double ltY = centerY + width / 2;

        return new LatLng(ltY, ltX);
    }
}
