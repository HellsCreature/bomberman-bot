package com.codenjoy.dojo.bomberman.client;

/*-
 * #%L
 * Codenjoy - it's a dojo-like platform from developers to developers.
 * %%
 * Copyright (C) 2018 Codenjoy
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.codenjoy.dojo.bomberman.model.Elements;
import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.sun.javafx.scene.web.Debugger;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.common.io.FrameFlusher;
import sun.util.locale.provider.LocaleServiceProviderPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.eclipse.jetty.http.MultiPartParser.LOG;

/**
 * User: your name
 */
public class YourSolver implements Solver<Board> {

    private Dice dice;
    private Board board;

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    // the method which should be implemented
    @Override
    public String get(Board board) {

        this.board = board;
        if (board.isMyBombermanDead()) return "";

        boolean isAct = true;
        int blastRadius = 3;

        ArrayList<Point> theWay = new ArrayList<>();

        //Arseniy: Задаём направления
        theWay.add(new PointImpl(board.getBomberman()));
        theWay.add(new PointImpl(board.getBomberman()));
        theWay.get(0).change(Direction.LEFT);
        theWay.add(new PointImpl(board.getBomberman()));
        theWay.get(1).change(Direction.UP);
        theWay.add(new PointImpl(board.getBomberman()));
        theWay.get(2).change(Direction.RIGHT);
        theWay.add(new PointImpl(board.getBomberman()));
        theWay.get(3).change(Direction.DOWN);

        System.out.println("init:" + theWay.toString());

        //Arseniy: Что нам может мешать
        Collection<Point> walls         = board.getWalls();
        Collection<Point> meatChoppers  = board.getMeatChoppers();
        Collection<Point> fMeatChoppers = board.getFutureMeatChoppers();
        Collection<Point> dWalls        = board.getDestroyableWalls();
        Collection<Point> oBombers      = board.getOtherBombermans();

        Collection<Point> bombs         = board.getBombs();
        Collection<Point> fBlasts       = board.getFutureBlasts();

        Collection<Point> bomb1         = board.get(Elements.BOMB_TIMER_1);
        System.out.println("bomb1:" + bomb1.toString());
        Collection<Point> bomb2         = board.get(Elements.BOMB_TIMER_2);
        System.out.println("bomb2:" + bomb2.toString());
        Collection<Point> bomb3         = board.get(Elements.BOMB_TIMER_3);
        System.out.println("bomb3:" + bomb3.toString());
        Collection<Point> bomb4         = board.get(Elements.BOMB_TIMER_4);
        System.out.println("bomb4:" + bomb4.toString());
        Collection<Point> bomb5         = board.get(Elements.BOMB_TIMER_5);
        bomb5.addAll(board.get(Elements.BOMB_BOMBERMAN));
        bomb5.addAll(board.get(Elements.OTHER_BOMB_BOMBERMAN));
        System.out.println("bomb5:" + bomb5.toString());

        Collection<Point> fBlasts1      = board.getFutureBlastsByCollection(bomb1);
        System.out.println("fBlasts1:" + fBlasts1.toString());
        Collection<Point> fBlasts2      = board.getFutureBlastsByCollection(bomb2);
        System.out.println("fBlasts2:" + fBlasts2.toString());
        Collection<Point> fBlasts3      = board.getFutureBlastsByCollection(bomb3);
        System.out.println("fBlasts3:" + fBlasts3.toString());
        Collection<Point> fBlasts4      = board.getFutureBlastsByCollection(bomb4);
        System.out.println("fBlasts4:" + fBlasts4.toString());
        Collection<Point> fBlasts5      = board.getFutureBlastsByCollection(bomb5);
        System.out.println("fBlasts5:" + fBlasts5.toString());

        //Arseniy: Значит ещё не стартовали
        if (meatChoppers.size() == 0) {
            return "";
        }

        //Arseniy: Убираем те направления, куда невозможно=>не нужно идти
        theWay.removeAll(walls);
        System.out.println("-walls:" + theWay.toString());
        theWay.removeAll(dWalls);
        System.out.println("-dWalls:" + theWay.toString());
        theWay.removeAll(oBombers);
        System.out.println("-oBombers:" + theWay.toString());
        theWay.removeAll(bombs);
        System.out.println("-bombs:" + theWay.toString());
        theWay.removeAll(meatChoppers);
        System.out.println("-choppers:" + theWay.toString());

        ArrayList<Point> theWaySafe = new ArrayList<>(theWay);

        theWaySafe.removeAll(fBlasts);
        System.out.println("-fBlasts:" + theWaySafe.toString());

        theWaySafe.removeAll(fMeatChoppers);
        System.out.println("-fChoppers:" + theWaySafe.toString());

        ArrayList<Point> finalWay;
        //Arseniy: Ок, у нас есть безопасный(е) направления, куда идти
        if (theWaySafe.size() > 0) {
            finalWay = new ArrayList<>(theWaySafe);
        } else {
            //Arseniy: вычисляем небезопасные, но и не смретельные направления

            ArrayList<Point> theWayLessSafe = new ArrayList<>(theWay);

            theWayLessSafe.removeAll(fBlasts1);
            theWayLessSafe.removeAll(fMeatChoppers);
            theWayLessSafe.removeAll(fBlasts2);
            theWayLessSafe.removeAll(fBlasts3);
            theWayLessSafe.removeAll(fBlasts4);
            theWayLessSafe.removeAll(fBlasts5);
            System.out.println("lessSafe-blast5:" + theWayLessSafe.toString());
            if (theWayLessSafe.size() > 0) {
                finalWay = new ArrayList<>(theWayLessSafe);
                System.out.println("finalWayBl5:" + finalWay.toString());
            } else {

                theWayLessSafe = new ArrayList<>(theWay);
                theWayLessSafe.removeAll(fBlasts1);
                theWayLessSafe.removeAll(fMeatChoppers);
                theWayLessSafe.removeAll(fBlasts2);
                theWayLessSafe.removeAll(fBlasts3);
                theWayLessSafe.removeAll(fBlasts4);
                System.out.println("lessSafe-blast4:" + theWayLessSafe.toString());
                if (theWayLessSafe.size() > 0) {
                    finalWay = new ArrayList<>(theWayLessSafe);
                    System.out.println("finalWayBl4:" + finalWay.toString());
                } else {

                    theWayLessSafe = new ArrayList<>(theWay);
                    theWayLessSafe.removeAll(fBlasts1);
                    theWayLessSafe.removeAll(fMeatChoppers);
                    theWayLessSafe.removeAll(fBlasts2);
                    theWayLessSafe.removeAll(fBlasts3);
                    System.out.println("lessSafe-blast3:" + theWayLessSafe.toString());
                    if (theWayLessSafe.size() > 0) {
                        finalWay = new ArrayList<>(theWayLessSafe);
                        System.out.println("finalWayBl3:" + finalWay.toString());
                    } else {

                        theWayLessSafe = new ArrayList<>(theWay);
                        theWayLessSafe.removeAll(fBlasts1);
                        theWayLessSafe.removeAll(fMeatChoppers);
                        theWayLessSafe.removeAll(fBlasts2);
                        System.out.println("lessSafe-blast2:" + theWayLessSafe.toString());
                        if (theWayLessSafe.size() > 0) {
                            finalWay = new ArrayList<>(theWayLessSafe);
                            System.out.println("finalWayBl2:" + finalWay.toString());
                        } else {

                            theWayLessSafe = new ArrayList<>(theWay);
                            theWayLessSafe.removeAll(fBlasts1);
                            theWayLessSafe.removeAll(fMeatChoppers);
                            System.out.println("lessSafe-fChoppers:" + theWayLessSafe.toString());
                            if (theWayLessSafe.size() > 0) {
                                finalWay = new ArrayList<>(theWayLessSafe);
                                System.out.println("finalWayfCHp:" + finalWay.toString());
                            } else {
                                theWayLessSafe = new ArrayList<>(theWay);
                                theWayLessSafe.removeAll(fBlasts1);
                                System.out.println("lessSafe-blast1:" + theWayLessSafe.toString());
                                if (theWayLessSafe.size() > 0) {
                                    finalWay = new ArrayList<>(theWayLessSafe);
                                    System.out.println("finalWayBl1:" + finalWay.toString());
                                } else {
                                    finalWay = new ArrayList<>(theWay);
                                }
                            }
                        }
                    }
                }
            }

            //Arseniy: если в итоге я во зрыве, то не идём к ближайшей бомбе
            if (fBlasts.contains(board.getBomberman()) & theWayLessSafe.size() > 1){
                double distance = 9999;
                Point bombXY = new PointImpl();
                //Aresniy: Ближайшая бомба
                for (Point bomb : bombs) {
                    if (board.getBomberman().distance(bomb) < distance) {
                        distance = board.getBomberman().distance(bomb);
                        bombXY = new PointImpl(bomb);
                    }
                }
                System.out.println("nearest bombXY:" + bombXY.toString());
                //Arseniy: направление к ближайшей бомбе
                Point wayXY = new PointImpl();
                distance = 9999;
                for (Point point : theWay) {
                    if (point.distance(bombXY) < distance){
                        distance = point.distance(bombXY);
                        wayXY = point;
                    }
                }
                System.out.println("wayXY:" + wayXY.toString());

                //Arseniy: удаляем это направление
                theWayLessSafe.remove(wayXY);
                System.out.println("removedToBomb:" + theWayLessSafe.toString());

                //Arseniy: во взрыве не стоит стоять, если можно идти
                if (theWayLessSafe.size() > 1) {
                    theWayLessSafe.remove(board.getBomberman());
                }
                if (theWayLessSafe.size() > 0) {
                    finalWay = new ArrayList<>(theWayLessSafe);
                } else {
                    finalWay = new ArrayList<>(theWay);
                }
            }
        }

        System.out.println("finalWay1:" + finalWay.toString());

        //Arseniy: Если больше одного пути, то не идти в тупик после установки бомбы
        if (isAct) {

            //Arseniy: ставим бомбу - значит не стоим на месте
            if (finalWay.size() > 1) {
                finalWay.remove(board.getBomberman());
            }

            //Arseniy: добавим взрывы от бомбы, которую поставим
            for (int i = 1; i < 3; i++) {
                fBlasts.add(new PointImpl(board.getBomberman().getX() + i, board.getBomberman().getY()));
                fBlasts.add(new PointImpl(board.getBomberman().getX() - i, board.getBomberman().getY()));
                fBlasts.add(new PointImpl(board.getBomberman().getX(), board.getBomberman().getY() + i));
                fBlasts.add(new PointImpl(board.getBomberman().getX(), board.getBomberman().getY() - i));
                bombs.add(new PointImpl(board.getBomberman().getX(), board.getBomberman().getY()));
            }

            //Arseniy: тупик ли один из возможных путей?
            //todo: тупик длиной с радиус взрыва
            if (finalWay.size() > 1) {
                for (int i = finalWay.size() - 1; i >=0; i-- ) {
                    ArrayList<Point> theFWay = new ArrayList<>();

                    System.out.println("future way:" + finalWay.get(i).toString());
                    //Arseniy: Задаём возможные направления после следующего хода
                    theFWay.add(new PointImpl(finalWay.get(i)));
                    theFWay.get(0).change(Direction.LEFT);
                    theFWay.add(new PointImpl(finalWay.get(i)));
                    theFWay.get(1).change(Direction.UP);
                    theFWay.add(new PointImpl(finalWay.get(i)));
                    theFWay.get(2).change(Direction.RIGHT);
                    theFWay.add(new PointImpl(finalWay.get(i)));
                    theFWay.get(3).change(Direction.DOWN);
                    System.out.println("fWayprob:" + theFWay.toString());
                    //Arseniy: Удаляем те, которые невозможны
                    theFWay.removeAll(walls);
                    theFWay.removeAll(dWalls);
                    theFWay.removeAll(bombs);
                    System.out.println("fWayact:" + theFWay.toString());

                    //Arseniy: Если в итоге пойти будет некуда
                    if (theFWay.size() <= 0 & finalWay.size() > 1) {
                        finalWay.remove(i);
                    }
                }
            }
        }

        System.out.println("finalWay2:" + finalWay.toString());

        String move;

        //Arseniy:
        Collections.shuffle(finalWay);

        System.out.println(finalWay.get(0).toString());
        System.out.println(finalWay.get(0).relative(board.getBomberman()).toString());

        switch (finalWay.get(0).relative(board.getBomberman()).toString()) {
            case "[1,0]":
                move = ",RIGHT";
                break;
            case "[-1,0]":
                move = ",LEFT";
                break;
            case "[0,1]":
                move = ",UP";
                break;
            case "[0,-1]":
                move = ",DOWN";
                break;
            default:
                move = "";
        }

        if (isAct) {
            return "ACT" + move;
        } else
        {
            return move;
        }

    }

    //private static

    /**
     * To connect to the game server:
     * 1. Sign up on the game server. If you did everything right, you'll get to the main game board.
     * 2. Click on your name on the right hand side panel
     * 3. Copy the whole link from the browser, paste it inside below method, now you're good to go!
     */
    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                /*"http://codenjoy.com:80/codenjoy-contest/board/player/3edq63tw0bq4w4iem7nb?code=1234567890123456789"*/
                "http://codingdojo.kz/codenjoy-contest/board/player/y28zy3gzg5wygjwd74qp?code=8030501113052592774",
                new YourSolver(new RandomDice()),
                new Board());
    }

}
