package com.pvpciv.client;

import java.util.ArrayList;
import java.util.List;

import static com.pvpciv.client.Mob3DLayer.Bone.*;
import static com.pvpciv.client.Mob3DLayer.c;
import static com.pvpciv.client.Mob3DLayer.r;
import static com.pvpciv.client.Mob3DLayer.wob;

/** Наборы объёмных деталей мобов этажа 3 (координаты — пиксели кости, перед — −Z). Цвета — плитки палитры (tools/mob3d_tex.py). */
public final class Mob3DParts {
    private Mob3DParts() {}
    private static final float P = (float) Math.PI;

    /** Самурай-ронин: кабуто с кувагатой, шейный щиток, маска-мэмпо, до, наплечники-содэ, кусадзури, ножны на бедре.
     *  0 — красный лак, 1 — золото, 2 — чёрный лак, 3 — железо, 4 — белый шнур. */
    public static List<Mob3DLayer.Cube> samurai() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 2, -4.5f, -9.2f, -4.5f, 9, 2.4f, 9));              // чаша шлема
        l.add(c(HEAD, 3, -4.6f, -8.2f, -4.6f, 9.2f, 0.5f, 9.2f));          // обод
        l.add(c(HEAD, 2, -5.2f, -7.2f, -5.4f, 10.4f, 0.8f, 1.6f));         // козырёк
        l.add(c(HEAD, 0, -5.4f, -7.4f, -4.2f, 1.1f, 5.2f, 8.6f));          // сикоро слева
        l.add(c(HEAD, 0, 4.3f, -7.4f, -4.2f, 1.1f, 5.2f, 8.6f));           // сикоро справа
        l.add(c(HEAD, 0, -5.2f, -7.4f, 3.6f, 10.4f, 5.6f, 1.4f));          // сикоро сзади
        l.add(c(HEAD, 4, -5.3f, -5.2f, 3.9f, 10.6f, 0.4f, 1.3f));          // шнуровка
        l.add(r(HEAD, 1, -1.2f, -9f, -5.2f, 0, 0, 0.42f, -1.7f, -15f, -5.4f, 1, 6.5f, 0.5f));   // кувагата левый
        l.add(r(HEAD, 1, 1.2f, -9f, -5.2f, 0, 0, -0.42f, 0.7f, -15f, -5.4f, 1, 6.5f, 0.5f));    // кувагата правый
        l.add(c(HEAD, 1, -1.2f, -10.2f, -5.6f, 2.4f, 2.4f, 0.6f));         // герб
        l.add(c(HEAD, 2, -3.6f, -3.6f, -4.7f, 7.2f, 3.2f, 0.8f));          // мэмпо
        l.add(c(HEAD, 4, -2.6f, -2.8f, -4.95f, 5.2f, 0.4f, 0.3f));         // «усы» мэмпо
        l.add(c(BODY, 0, -4.6f, 0.4f, -2.7f, 9.2f, 8.5f, 0.9f));           // до спереди
        l.add(c(BODY, 0, -4.6f, 0.4f, 1.8f, 9.2f, 8.5f, 0.9f));            // до сзади
        for (int i = 0; i < 3; i++) l.add(c(BODY, 1, -4.7f, 2 + i * 2.4f, -2.85f, 9.4f, 0.4f, 0.3f));   // золотые шнуры
        l.add(c(BODY, 2, -4.8f, 8.9f, -2.9f, 9.6f, 1.2f, 5.8f));           // пояс
        l.add(r(BODY, 0, -2.3f, 10f, -2.9f, -0.12f, 0, 0, -4.6f, 10f, -3.1f, 4.4f, 5.2f, 0.7f));   // кусадзури
        l.add(r(BODY, 0, 2.3f, 10f, -2.9f, -0.12f, 0, 0, 0.2f, 10f, -3.1f, 4.4f, 5.2f, 0.7f));
        l.add(r(BODY, 0, 0, 10f, 2.9f, 0.12f, 0, 0, -4.6f, 10f, 2.4f, 9.2f, 5.2f, 0.7f));
        l.add(r(BODY, 2, 4.6f, 9.5f, 0, -0.55f, 0, 0, 4.4f, 9f, -6f, 1.1f, 1.3f, 13f));   // ножны на левом бедре
        l.add(r(BODY, 4, 4.6f, 9.5f, 0, -0.55f, 0, 0, 4.35f, 8.9f, -9.2f, 1.2f, 1.5f, 3.2f));   // рукоять
        l.add(r(RARM, 0, -1f, -2f, 0, 0, 0, 0.18f, -4.2f, -2.8f, -2.8f, 1.6f, 5.4f, 5.6f));  // содэ
        l.add(r(LARM, 0, 1f, -2f, 0, 0, 0, -0.18f, 2.6f, -2.8f, -2.8f, 1.6f, 5.4f, 5.6f));
        l.add(c(RARM, 3, -3.3f, 6.5f, -2.3f, 4.6f, 2.6f, 4.6f));           // котэ
        l.add(c(LARM, 3, -1.3f, 6.5f, -2.3f, 4.6f, 2.6f, 4.6f));
        l.add(c(RLEG, 3, -2.3f, 6f, -2.4f, 4.6f, 4.5f, 0.8f));             // сунэатэ
        l.add(c(LLEG, 3, -2.3f, 6f, -2.4f, 4.6f, 4.5f, 0.8f));
        return l;
    }

    /** Ниндзя: повязка с длинными хвостами, шарф, катана за спиной, наручи. 0 — тёмно-синий, 1 — красный, 2 — сталь, 3 — серый. */
    public static List<Mob3DLayer.Cube> ninja() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 1, -4.4f, -6.6f, -4.4f, 8.8f, 1.3f, 8.8f));          // повязка
        l.add(c(HEAD, 2, -1.2f, -6.8f, -4.7f, 2.4f, 1.6f, 0.5f));           // металлическая пластина
        l.add(wob(r(HEAD, 1, -1f, -6f, 4.3f, 0.55f, 0.15f, 0, -1.6f, -6.2f, 4.3f, 1.2f, 0.5f, 7f), 1));   // хвосты повязки
        l.add(wob(r(HEAD, 1, 1f, -6f, 4.3f, 0.75f, -0.2f, 0, 0.4f, -6.2f, 4.3f, 1.2f, 0.5f, 6f), 1));
        l.add(c(BODY, 3, -4.5f, -0.2f, -2.5f, 9, 2.2f, 5));                // шарф
        l.add(r(BODY, 0, 0, 5f, 2.4f, 0, 0, 0.65f, -0.7f, -4f, 2.4f, 1.4f, 15f, 1.2f));   // ножны за спиной
        l.add(r(BODY, 1, 0, 5f, 2.4f, 0, 0, 0.65f, -0.8f, -7.6f, 2.3f, 1.6f, 4f, 1.4f));  // рукоять
        l.add(r(BODY, 2, 0, 5f, 2.4f, 0, 0, 0.65f, -1.4f, -3.9f, 2.2f, 2.8f, 0.6f, 1.6f)); // цуба
        l.add(c(RARM, 3, -3.3f, 5f, -2.3f, 4.6f, 4f, 4.6f));              // наручи
        l.add(c(LARM, 3, -1.3f, 5f, -2.3f, 4.6f, 4f, 4.6f));
        l.add(c(RLEG, 3, -2.3f, 8f, -2.3f, 4.6f, 3f, 4.6f));               // обмотки
        l.add(c(LLEG, 3, -2.3f, 8f, -2.3f, 4.6f, 3f, 4.6f));
        return l;
    }

    /** Асигару-лучник: дзингаса, до с гербом, колчан со стрелами. 0 — соломенный, 1 — тёмная кожа, 2 — красный, 3 — дерево, 4 — оперение. */
    public static List<Mob3DLayer.Cube> ashigaru() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 0, -7.5f, -8.8f, -7.5f, 15, 0.7f, 15));              // поля шляпы
        l.add(c(HEAD, 0, -5.5f, -9.6f, -5.5f, 11, 0.8f, 11));
        l.add(c(HEAD, 0, -3.5f, -10.4f, -3.5f, 7, 0.8f, 7));
        l.add(c(HEAD, 0, -1.5f, -11.1f, -1.5f, 3, 0.7f, 3));
        l.add(c(HEAD, 2, -1, -10.6f, -3.8f, 2, 1.4f, 0.4f));               // герб на шляпе
        l.add(c(HEAD, 1, -4.2f, -2.2f, -4.3f, 8.4f, 0.5f, 8.6f));          // ремешок
        l.add(c(BODY, 1, -4.5f, 0.6f, -2.6f, 9, 8.6f, 0.7f));              // до
        l.add(c(BODY, 2, -1.2f, 2.6f, -2.85f, 2.4f, 2.4f, 0.3f));          // мон
        l.add(c(BODY, 1, -4.6f, 9f, -2.8f, 9.2f, 1, 5.6f));                // пояс
        l.add(r(BODY, 3, 0, 4f, 2.2f, 0, 0, -0.35f, -0.5f, -2f, 2.2f, 3.2f, 10f, 2.6f));   // колчан
        for (int i = 0; i < 3; i++) l.add(r(BODY, 4, 0, 4f, 2.2f, 0, 0, -0.35f, -0.2f + i * 0.9f, -4.4f, 2.6f, 0.7f, 2.4f, 1.8f));   // оперение
        l.add(r(RARM, 1, -1f, -2f, 0, 0, 0, 0.12f, -3.9f, -2.4f, -2.5f, 1.2f, 3.6f, 5f));   // наплечники
        l.add(r(LARM, 1, 1f, -2f, 0, 0, 0, -0.12f, 2.7f, -2.4f, -2.5f, 1.2f, 3.6f, 5f));
        return l;
    }

    /** Оборотень: морда с пастью и клыками, уши, грива, шерсть на спине, когти, хвост. 0 — тёмный мех, 1 — светлый мех, 2 — нос, 3 — кость, 4 — красная пасть. */
    public static List<Mob3DLayer.Cube> werewolf() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 1, -2.2f, -4.6f, -9f, 4.4f, 3.2f, 5.2f));            // морда
        l.add(c(HEAD, 0, -2.6f, -5.4f, -6.2f, 5.2f, 1.2f, 2.4f));          // переносица
        l.add(c(HEAD, 2, -1.1f, -4.9f, -9.4f, 2.2f, 1.3f, 1));             // нос
        l.add(c(HEAD, 4, -2f, -1.6f, -8.4f, 4, 0.5f, 4.4f));               // пасть
        l.add(c(HEAD, 0, -2f, -1.3f, -8.2f, 4, 1.4f, 4.2f));               // нижняя челюсть
        l.add(c(HEAD, 3, -1.8f, -2.1f, -8.9f, 0.6f, 1.1f, 0.6f));          // клыки
        l.add(c(HEAD, 3, 1.2f, -2.1f, -8.9f, 0.6f, 1.1f, 0.6f));
        l.add(wob(r(HEAD, 0, -3f, -8f, 0, 0, 0, -0.25f, -4.2f, -12.2f, -1f, 2.4f, 4.4f, 1.4f), 2));   // уши
        l.add(wob(r(HEAD, 0, 3f, -8f, 0, 0, 0, 0.25f, 1.8f, -12.2f, -1f, 2.4f, 4.4f, 1.4f), 2));
        l.add(c(HEAD, 0, -4.8f, -6f, -3.6f, 1.2f, 4.5f, 6));               // бакенбарды
        l.add(c(HEAD, 0, 3.6f, -6f, -3.6f, 1.2f, 4.5f, 6));
        l.add(c(BODY, 0, -5.4f, -1.4f, -3.2f, 10.8f, 4.2f, 6.6f));         // грива
        l.add(c(BODY, 1, -3f, 1.6f, -2.8f, 6, 5, 0.8f));                   // светлая грудь
        l.add(r(BODY, 0, 0, 3, 2, -0.35f, 0, 0, -2.5f, 2, 2, 5, 3.5f, 2.2f));   // шерсть на спине
        l.add(r(BODY, 0, 0, 7, 2, -0.35f, 0, 0, -2f, 6, 2, 4, 3, 1.8f));
        l.add(wob(r(BODY, 0, 0, 10.5f, 2, -0.55f, 0, 0, -1.4f, 9.6f, 1.8f, 2.8f, 2.8f, 9f), 1));   // хвост (вниз-назад)
        l.add(wob(r(BODY, 1, 0, 10.5f, 2, -0.55f, 0, 0, -1.1f, 9.9f, 10.6f, 2.2f, 2.2f, 2.5f), 1));
        for (int i = 0; i < 3; i++) {                                        // когти
            l.add(c(RARM, 3, -2.8f + i * 1.3f, 9.6f, -2.6f, 0.6f, 2.2f, 0.8f));
            l.add(c(LARM, 3, -0.8f + i * 1.3f, 9.6f, -2.6f, 0.6f, 2.2f, 0.8f));
        }
        l.add(c(RARM, 0, -3.5f, -2.6f, -2.5f, 5, 3, 5));                  // мех на плечах
        l.add(c(LARM, 0, -1.5f, -2.6f, -2.5f, 5, 3, 5));
        return l;
    }

    /** Лучница в маске кицунэ: маска-морда, уши, три хвоста, колчан. 0 — белый, 1 — красный, 2 — рыжий мех, 3 — чёрный, 4 — золото. */
    public static List<Mob3DLayer.Cube> kitsune() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 0, -1.8f, -5.2f, -7.2f, 3.6f, 2.8f, 3.2f));          // мордочка маски
        l.add(c(HEAD, 3, -0.6f, -4.9f, -7.5f, 1.2f, 0.9f, 0.5f));          // нос
        l.add(c(HEAD, 1, -2.2f, -6.4f, -4.6f, 1, 0.5f, 0.4f));             // красные метки
        l.add(c(HEAD, 1, 1.2f, -6.4f, -4.6f, 1, 0.5f, 0.4f));
        l.add(c(HEAD, 1, -0.4f, -7.6f, -4.6f, 0.8f, 1.6f, 0.4f));
        l.add(wob(r(HEAD, 0, -2.5f, -8f, 0, 0, 0, -0.2f, -4f, -11.6f, -1.2f, 2.6f, 3.8f, 1.2f), 2));   // уши
        l.add(wob(r(HEAD, 0, 2.5f, -8f, 0, 0, 0, 0.2f, 1.4f, -11.6f, -1.2f, 2.6f, 3.8f, 1.2f), 2));
        l.add(wob(r(HEAD, 1, -2.5f, -8f, 0, 0, 0, -0.2f, -3.5f, -10.8f, -1.35f, 1.6f, 2.4f, 0.3f), 2));
        l.add(wob(r(HEAD, 1, 2.5f, -8f, 0, 0, 0, 0.2f, 1.9f, -10.8f, -1.35f, 1.6f, 2.4f, 0.3f), 2));
        l.add(c(HEAD, 1, -4.3f, -1.4f, -4.3f, 8.6f, 0.6f, 8.6f));          // шнур маски
        for (int i = 0; i < 3; i++) {                                        // три хвоста
            float yr = (i - 1) * 0.45f;
            l.add(wob(r(BODY, 2, 0, 10.5f, 2, 0.95f, yr, 0, -1.6f, 9.4f, 1.8f, 3.2f, 3.2f, 10f), 1));
            l.add(wob(r(BODY, 0, 0, 10.5f, 2, 0.95f, yr, 0, -1.3f, 9.7f, 11.6f, 2.6f, 2.6f, 2.6f), 1));
        }
        l.add(r(BODY, 1, 0, 4f, 2.2f, 0, 0, -0.35f, -0.5f, -2f, 2.2f, 3.2f, 10f, 2.6f));   // колчан
        for (int i = 0; i < 3; i++) l.add(r(BODY, 0, 0, 4f, 2.2f, 0, 0, -0.35f, -0.2f + i * 0.9f, -4.4f, 2.6f, 0.7f, 2.4f, 1.8f));
        l.add(c(BODY, 1, -4.6f, 8.6f, -2.7f, 9.2f, 1.2f, 5.4f));           // оби
        l.add(c(BODY, 4, -1f, 8.4f, -2.95f, 2, 1.6f, 0.4f));
        return l;
    }

    // ================================================================ 0.11: этаж 4 — НТЦ «NaZa»
    /** Ботаник: респиратор, цветок на голове, лианы поверх халата. 0 — халат, 1 — листья, 2 — лиана, 3 — цветок, 4 — фильтр. */
    public static List<Mob3DLayer.Cube> botanist() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 4, -2.4f, -3.4f, -5.1f, 4.8f, 2.6f, 1.3f));           // респиратор
        l.add(c(HEAD, 2, -1, -3f, -5.6f, 2, 1.6f, 0.6f));
        l.add(c(HEAD, 3, -2f, -10f, -1f, 4, 1.4f, 4));                     // цветок
        l.add(c(HEAD, 3, -1f, -10.6f, -2.2f, 2, 1.2f, 6.4f));
        l.add(c(HEAD, 1, -0.5f, -9f, -0.5f, 1, 1.2f, 1));
        l.add(wob(r(HEAD, 1, -4f, -8f, 0, 0, 0, -0.4f, -6.2f, -9f, -1.5f, 3, 0.8f, 3), 2));   // листья
        l.add(wob(r(HEAD, 1, 4f, -8f, 0, 0, 0, 0.4f, 3.2f, -9f, -1.5f, 3, 0.8f, 3), 2));
        l.add(c(BODY, 0, -4.4f, 9f, 1.7f, 8.8f, 6.5f, 0.7f));               // полы халата
        l.add(c(BODY, 2, -4.4f, 3f, -2.4f, 8.8f, 0.9f, 4.8f));              // лианы
        l.add(r(BODY, 2, 0, 6, 0, 0, 0, 0.55f, -5.2f, 5.6f, -2.5f, 10.4f, 0.8f, 5));
        l.add(c(BODY, 1, 2.2f, 4.6f, -2.9f, 2, 1.4f, 0.5f));
        l.add(c(BODY, 1, -3.4f, 7.4f, -2.9f, 1.8f, 1.2f, 0.5f));
        l.add(c(RARM, 1, -3.6f, -2.8f, -2.5f, 4.8f, 1.4f, 5));             // листья на плечах
        l.add(c(LARM, 1, -1.2f, -2.8f, -2.5f, 4.8f, 1.4f, 5));
        l.add(c(RARM, 2, -3.3f, 4f, -2.3f, 4.6f, 0.8f, 4.6f));
        return l;
    }

    /** Размороженный образец: ледяная корка, кристаллы, бирка образца. 0 — лёд, 1 — тёмный лёд, 2 — иней, 3 — тёмно-синий, 4 — жёлтая бирка. */
    public static List<Mob3DLayer.Cube> thawed() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 2, -4.4f, -8.5f, -4.4f, 8.8f, 1.1f, 8.8f));          // иней на макушке
        l.add(r(HEAD, 0, -2, -8, 0, 0, 0, -0.4f, -3f, -11.5f, -1f, 1.6f, 3.8f, 1.6f));   // кристаллы
        l.add(r(HEAD, 1, 2, -8, 1, 0.3f, 0, 0.35f, 1.4f, -12f, 0f, 1.4f, 4.2f, 1.4f));
        l.add(r(HEAD, 0, 0, -8, 2, -0.3f, 0, 0, -0.7f, -11f, 1.5f, 1.4f, 3.2f, 1.4f));
        l.add(c(HEAD, 0, -4.6f, -5f, -4.7f, 3, 2.4f, 0.6f));              // корка на лице
        l.add(c(BODY, 0, -4.6f, 1f, -2.7f, 6, 5, 0.8f));                    // ледяные пластины
        l.add(c(BODY, 1, 0.6f, 5.4f, -2.7f, 4, 4.6f, 0.8f));
        l.add(c(BODY, 4, 1.2f, 2.4f, -2.85f, 2.2f, 1.4f, 0.3f));          // бирка
        l.add(r(BODY, 0, 0, 2, 2, -0.5f, 0, 0.3f, -3f, 0f, 2f, 1.6f, 4.6f, 1.6f));
        l.add(r(BODY, 1, 0, 2, 2, -0.6f, 0, -0.3f, 1.4f, 0.4f, 2f, 1.4f, 4f, 1.4f));
        l.add(c(RARM, 1, -3.8f, -4f, -1.4f, 2.6f, 4f, 2.6f));            // кристаллы на плечах
        l.add(c(LARM, 0, 1.2f, -4.4f, -1.1f, 2.4f, 4.6f, 2.4f));
        l.add(c(RLEG, 2, -2.3f, 9f, -2.3f, 4.6f, 3, 4.6f));
        l.add(c(LLEG, 2, -2.3f, 9f, -2.3f, 4.6f, 3, 4.6f));
        return l;
    }

    /** Робот-техник: корпус-шлем с визором и антенной, нагрудник с ядром, наплечники. 0 — сталь, 1 — оранжевое свечение, 2 — тёмный, 3 — хром, 4 — жёлтые полосы. */
    public static List<Mob3DLayer.Cube> robot() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 3, -4.5f, -8.5f, -4.5f, 9, 8.6f, 9));               // корпус головы
        l.add(c(HEAD, 1, -3.5f, -5.6f, -4.85f, 7, 1.4f, 0.5f));           // визор
        l.add(c(HEAD, 2, -2.5f, -2.6f, -4.85f, 5, 1.6f, 0.5f));           // решётка
        l.add(c(HEAD, 2, -0.3f, -11.5f, -0.3f, 0.6f, 3, 0.6f));            // антенна
        l.add(c(HEAD, 1, -0.6f, -12.3f, -0.6f, 1.2f, 1, 1.2f));
        l.add(c(HEAD, 2, -5.1f, -6f, -1.5f, 0.8f, 3, 3));                 // «уши»-болты
        l.add(c(HEAD, 2, 4.3f, -6f, -1.5f, 0.8f, 3, 3));
        l.add(c(BODY, 0, -4.7f, 0.2f, -2.8f, 9.4f, 9, 5.6f));              // нагрудник
        l.add(c(BODY, 1, -1.2f, 2.8f, -3.05f, 2.4f, 2.4f, 0.4f));          // ядро
        l.add(c(BODY, 2, -3f, 2f, 2.8f, 6, 5.5f, 1));                      // вентиляция
        l.add(c(BODY, 4, -4.8f, 9.2f, -2.9f, 9.6f, 1.2f, 5.8f));
        l.add(c(RARM, 0, -4f, -3f, -3f, 6, 4, 6));                        // наплечники
        l.add(c(LARM, 0, -2f, -3f, -3f, 6, 4, 6));
        l.add(c(RARM, 4, -4.1f, -1.2f, -3.1f, 6.2f, 0.8f, 6.2f));
        l.add(c(LARM, 4, -2.1f, -1.2f, -3.1f, 6.2f, 0.8f, 6.2f));
        l.add(c(RARM, 2, -3.4f, 5f, -2.4f, 4.8f, 5, 4.8f));               // предплечья
        l.add(c(LARM, 2, -1.4f, 5f, -2.4f, 4.8f, 5, 4.8f));
        l.add(c(RLEG, 0, -2.4f, 6f, -2.4f, 4.8f, 6, 4.8f));
        l.add(c(LLEG, 0, -2.4f, 6f, -2.4f, 4.8f, 6, 4.8f));
        return l;
    }

    /** Химера: морда с клыками, рога, шипы по хребту, кислотный мешок, хвост-жало, когти. 0 — фиолетовая шкура, 1 — кислота, 2 — кость, 3 — тёмный, 4 — красные глаза. */
    public static List<Mob3DLayer.Cube> chimera() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 0, -2.5f, -4.6f, -8.2f, 5, 3.6f, 4.2f));             // морда
        l.add(c(HEAD, 3, -2.2f, -1.2f, -7.8f, 4.4f, 1.2f, 3.8f));          // челюсть
        l.add(c(HEAD, 2, -2.2f, -1.6f, -8.3f, 0.6f, 1.1f, 0.6f));          // клыки
        l.add(c(HEAD, 2, 1.6f, -1.6f, -8.3f, 0.6f, 1.1f, 0.6f));
        l.add(c(HEAD, 4, -3.2f, -6f, -4.7f, 1.6f, 0.9f, 0.4f));            // глаза (по два)
        l.add(c(HEAD, 4, 1.6f, -6f, -4.7f, 1.6f, 0.9f, 0.4f));
        l.add(c(HEAD, 4, -2.6f, -7.2f, -4.7f, 1f, 0.7f, 0.4f));
        l.add(c(HEAD, 4, 1.6f, -7.2f, -4.7f, 1f, 0.7f, 0.4f));
        l.add(r(HEAD, 2, -3, -8, -1, 0.4f, 0, -0.55f, -4.8f, -13f, -1.8f, 1.6f, 5.4f, 1.6f));   // рога
        l.add(r(HEAD, 2, 3, -8, -1, 0.4f, 0, 0.55f, 3.2f, -13f, -1.8f, 1.6f, 5.4f, 1.6f));
        for (int i = 0; i < 4; i++) l.add(r(BODY, 2, 0, 1 + i * 3, 2, -0.6f, 0, 0, -0.6f, -0.6f + i * 3, 2f, 1.2f, 3.6f, 1.2f));   // шипы
        l.add(c(BODY, 1, -3f, 4f, -3f, 6, 4.2f, 1));                       // кислотный мешок
        l.add(c(BODY, 0, -5f, -1f, -3f, 10, 3.2f, 6));                     // загривок
        l.add(wob(r(BODY, 0, 0, 10.5f, 2, -0.55f, 0, 0, -1.4f, 9.6f, 1.8f, 2.8f, 2.8f, 10f), 1));   // хвост
        l.add(wob(r(BODY, 1, 0, 10.5f, 2, -0.55f, 0, 0, -1f, 10f, 11.8f, 2f, 2f, 2.6f), 1));        // жало
        for (int i = 0; i < 3; i++) {
            l.add(c(RARM, 2, -2.8f + i * 1.3f, 9.6f, -2.6f, 0.6f, 2.4f, 0.8f));
            l.add(c(LARM, 2, -0.8f + i * 1.3f, 9.6f, -2.6f, 0.6f, 2.4f, 0.8f));
        }
        l.add(c(RARM, 0, -3.6f, -2.8f, -2.6f, 5.2f, 3.2f, 5.2f));
        l.add(c(LARM, 0, -1.6f, -2.8f, -2.6f, 5.2f, 3.2f, 5.2f));
        return l;
    }

    /** Охранник NaZa: шлем с голубым визором, бронежилет, наплечники. 0 — тёмно-синий, 1 — голубое свечение, 2 — графит, 3 — белый. */
    public static List<Mob3DLayer.Cube> guard() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 0, -4.6f, -8.8f, -4.6f, 9.2f, 4, 9.2f));             // шлем
        l.add(c(HEAD, 1, -4.2f, -5.4f, -4.95f, 8.4f, 1.6f, 0.6f));         // визор
        l.add(c(HEAD, 2, -4.4f, -2f, -4.75f, 8.8f, 2, 1));                 // подбородник
        l.add(c(HEAD, 3, -0.6f, -9.3f, -4.7f, 1.2f, 1.2f, 9.4f));          // гребень
        l.add(c(HEAD, 2, 3.6f, -11f, 1f, 0.6f, 3, 0.6f));                  // антенна рации
        l.add(c(BODY, 0, -4.6f, 0.5f, -2.7f, 9.2f, 9, 5.4f));              // жилет
        l.add(c(BODY, 1, -1f, 2.4f, -2.95f, 2, 2, 0.3f));                  // эмблема
        l.add(c(BODY, 3, -4f, 6f, -2.95f, 2.4f, 1.2f, 0.3f));
        l.add(c(BODY, 2, -4.7f, 9.2f, -2.8f, 9.4f, 1.2f, 5.6f));           // пояс
        l.add(c(RARM, 0, -3.6f, -2.6f, -2.6f, 5.2f, 3.5f, 5.2f));          // наплечники
        l.add(c(LARM, 0, -1.6f, -2.6f, -2.6f, 5.2f, 3.5f, 5.2f));
        l.add(c(RLEG, 2, -2.3f, 4f, -2.5f, 4.6f, 2.5f, 0.8f));             // наколенники
        l.add(c(LLEG, 2, -2.3f, 4f, -2.5f, 4.6f, 2.5f, 0.8f));
        return l;
    }

    /** Лаборант: очки, воротник и полы халата, баллон с плазмой за спиной. 0 — белый халат, 1 — фиолетовое свечение, 2 — тёмный, 3 — стекло очков. */
    public static List<Mob3DLayer.Cube> labtech() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(HEAD, 2, -4.3f, -5.6f, -4.3f, 8.6f, 1.2f, 8.6f));          // ремешок очков
        l.add(c(HEAD, 3, -3.4f, -6f, -4.9f, 2.6f, 2, 0.8f));               // линзы
        l.add(c(HEAD, 3, 0.8f, -6f, -4.9f, 2.6f, 2, 0.8f));
        l.add(c(BODY, 0, -4.4f, -0.4f, -2.5f, 8.8f, 1.4f, 5));             // воротник
        l.add(c(BODY, 0, -4.4f, 9f, 1.7f, 8.8f, 6.5f, 0.7f));              // полы
        l.add(c(BODY, 0, -4.4f, 9f, -2.6f, 3.6f, 6.5f, 0.6f));
        l.add(c(BODY, 0, 0.8f, 9f, -2.6f, 3.6f, 6.5f, 0.6f));
        l.add(c(BODY, 2, -2f, 1f, 2.2f, 4, 8, 2.6f));                      // баллон
        l.add(c(BODY, 1, -1f, 2f, 4.7f, 2, 6, 0.4f));
        l.add(c(BODY, 1, 2.2f, 2f, -2.85f, 1.4f, 2.2f, 0.3f));            // бейдж
        return l;
    }

    /** Турель: опора-тренога (кость тела) и поворотная башня с двумя стволами (кость головы). 0 — сталь, 1 — светлая сталь, 2 — красная линза, 3 — чёрный, 4 — жёлтый. */
    public static List<Mob3DLayer.Cube> turret() {
        List<Mob3DLayer.Cube> l = new ArrayList<>();
        l.add(c(BODY, 0, -6f, 22.5f, -6f, 12, 1.5f, 12));                  // опорная плита
        l.add(c(BODY, 3, -8f, 22f, -1f, 16, 1.6f, 2));                     // лапы
        l.add(c(BODY, 3, -1f, 22f, -8f, 2, 1.6f, 16));
        l.add(c(BODY, 1, -2f, 3f, -2f, 4, 19.5f, 4));                      // колонна
        l.add(c(BODY, 4, -3f, 17f, -3f, 6, 1.2f, 6));                      // полоса
        l.add(c(BODY, 4, -3f, 8f, -3f, 6, 1.2f, 6));
        l.add(c(BODY, 0, -4.5f, 0.5f, -4.5f, 9, 3, 9));                    // поворотный круг
        l.add(c(HEAD, 0, -5f, -7f, -5f, 10, 7, 10));                       // корпус башни
        l.add(c(HEAD, 1, -4f, -8f, -4f, 8, 1, 8));
        l.add(c(HEAD, 3, -3f, -4.6f, -14f, 2, 2, 9));                      // стволы
        l.add(c(HEAD, 3, 1f, -4.6f, -14f, 2, 2, 9));
        l.add(c(HEAD, 1, -3.6f, -5.2f, -15f, 7.2f, 3.2f, 1.5f));           // дульный тормоз
        l.add(c(HEAD, 2, -1.5f, -6.6f, -5.5f, 3, 1.5f, 0.6f));             // линза
        l.add(c(HEAD, 4, 5f, -6f, -3f, 2, 4, 6));                          // короба с лентой
        l.add(c(HEAD, 4, -7f, -6f, -3f, 2, 4, 6));
        return l;
    }

    /** Для брони игрока: только кости из набора, увеличенные (поверх ванильного слоя брони, он раздут на 1 пиксель). */
    public static List<Mob3DLayer.Cube> forArmor(List<Mob3DLayer.Cube> src, float k, Mob3DLayer.Bone... bones) {
        java.util.Set<Mob3DLayer.Bone> set = java.util.EnumSet.noneOf(Mob3DLayer.Bone.class);
        java.util.Collections.addAll(set, bones);
        List<Mob3DLayer.Cube> out = new ArrayList<>();
        for (Mob3DLayer.Cube c : src) {
            if (!set.contains(c.bone())) continue;
            out.add(new Mob3DLayer.Cube(c.bone(), c.color(), c.x() * k, c.y() * k, c.z() * k, c.w() * k, c.h() * k, c.d() * k,
                    c.px() * k, c.py() * k, c.pz() * k, c.rx(), c.ry(), c.rz(), c.wob()));
        }
        return out;
    }
}
