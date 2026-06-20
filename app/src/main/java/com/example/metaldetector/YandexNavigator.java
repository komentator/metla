package com.example.metaldetector;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

public class YandexNavigator {

    private static final String YANDEX_MAPS_PACKAGE = "ru.yandex.yandexmaps";
    private static final String YANDEX_NAVI_PACKAGE = "ru.yandex.yandexnavi";

    /**
     * Открыть Яндекс.Карты на указанных координатах.
     * Если приложение не установлено — открывает в браузере.
     */
    public static void openYandexMaps(Context context, double lat, double lon, int zoom) {
        Uri uri = Uri.parse("yandexmaps://maps.yandex.ru/?ll=" + lon + "," + lat + "&z=" + zoom + "&pt=" + lon + "," + lat + "~pm2rdl");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(YANDEX_MAPS_PACKAGE);

        if (!isIntentAvailable(context, intent)) {
            // Fallback: открыть в браузере
            uri = Uri.parse("https://yandex.ru/maps/?ll=" + lon + "," + lat + "&z=" + zoom + "&pt=" + lon + "," + lat);
            intent = new Intent(Intent.ACTION_VIEW, uri);
        }

        context.startActivity(intent);
    }

    /**
     * Открыть Яндекс.Навигатор с построением маршрута от точки A до точки B.
     * Если навигатор не установлен — fallback на Яндекс.Карты в браузере.
     */
    public static void buildRoute(Context context, double latFrom, double lonFrom, double latTo, double lonTo) {
        Uri uri = Uri.parse("yandexnavi://build_route_on_map?lat_from=" + latFrom + "&lon_from=" + lonFrom
                + "&lat_to=" + latTo + "&lon_to=" + lonTo);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(YANDEX_NAVI_PACKAGE);

        if (!isIntentAvailable(context, intent)) {
            // Fallback: маршрут в Яндекс.Картах через браузер
            uri = Uri.parse("https://yandex.ru/maps/?rtext=" + latFrom + "," + lonFrom + "~" + latTo + "," + lonTo + "&rtt=auto");
            intent = new Intent(Intent.ACTION_VIEW, uri);
        }

        context.startActivity(intent);
    }

    /**
     * Открыть Яндекс.Навигатор на точке (например, чтобы доехать до неё).
     * Если точка отправления неизвестна — используем текущую позицию навигатора.
     */
    public static void navigateTo(Context context, double latTo, double lonTo) {
        Uri uri = Uri.parse("yandexnavi://build_route_on_map?lat_to=" + latTo + "&lon_to=" + lonTo);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage(YANDEX_NAVI_PACKAGE);

        if (!isIntentAvailable(context, intent)) {
            uri = Uri.parse("https://yandex.ru/maps/?rtext=~" + latTo + "," + lonTo + "&rtt=auto");
            intent = new Intent(Intent.ACTION_VIEW, uri);
        }

        context.startActivity(intent);
    }

    /**
     * Проверить, установлено ли приложение.
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static boolean isIntentAvailable(Context context, Intent intent) {
        return intent.resolveActivity(context.getPackageManager()) != null;
    }
}
