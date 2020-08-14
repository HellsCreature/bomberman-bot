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
import com.codenjoy.dojo.client.AbstractLayeredBoard;
import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.sun.javafx.scene.web.Debugger;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.websocket.common.io.FrameFlusher;
import sun.util.locale.provider.LocaleServiceProviderPool;

import javax.swing.text.html.parser.Entity;
import java.util.*;

import static org.eclipse.jetty.http.MultiPartParser.LOG;

/**
 * User: your name
 */
public class YourSolver implements Solver<Board> {

    private Dice dice;
    private Board board;
    private ArrayList<Point> bestPointsGlob = new ArrayList<>();

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    // the method which should be implemented
    @Override
    public String get(Board board) {

        this.board = board;
        if (board.isMyBombermanDead()) return "";

        boolean isAct = false;
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
        Collection<Point> bomb2         = board.get(Elements.BOMB_TIMER_2);
        Collection<Point> bomb3         = board.get(Elements.BOMB_TIMER_3);
        Collection<Point> bomb4         = board.get(Elements.BOMB_TIMER_4);
        Collection<Point> bomb5         = board.get(Elements.BOMB_TIMER_5);
        bomb5.addAll(board.get(Elements.BOMB_BOMBERMAN));
        bomb5.addAll(board.get(Elements.OTHER_BOMB_BOMBERMAN));

        Collection<Point> fBlasts1      = board.getFutureBlastsByCollection(bomb1);
        Collection<Point> fBlasts2      = board.getFutureBlastsByCollection(bomb2);
        Collection<Point> fBlasts3      = board.getFutureBlastsByCollection(bomb3);
        Collection<Point> fBlasts4      = board.getFutureBlastsByCollection(bomb4);
        Collection<Point> fBlasts5      = board.getFutureBlastsByCollection(bomb5);

        System.out.println("bomb1:" + bomb1.toString());
        System.out.println("bomb2:" + bomb2.toString());
        System.out.println("bomb3:" + bomb3.toString());
        System.out.println("bomb4:" + bomb4.toString());
        System.out.println("bomb5:" + bomb5.toString());
        System.out.println("fBlasts1:" + fBlasts1.toString());
        System.out.println("fBlasts2:" + fBlasts2.toString());
        System.out.println("fBlasts3:" + fBlasts3.toString());
        System.out.println("fBlasts4:" + fBlasts4.toString());
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

        //Arseniy: считаем будущие пути и затраты/бонусы от них
        HashMap<Point, Step> newWay = new HashMap<>();
        ArrayList<Point> bomberman = new ArrayList<>();
        bomberman.add(board.getBomberman());
        addNewStep(newWay, bomberman, board.getBomberman(), 0);
        System.out.println("newWay:" + newWay.toString());

        int bestCost = -9999;
        Step step;

        //Arseniy: ставим, если пришли в лучшую точку
        System.out.println("bestPointsGlob:" + bestPointsGlob.toString());
        if (bestPointsGlob.contains(board.getBomberman())) {
            isAct = true;
        } else {
            isAct = false;
        }

        //Arseniy: если ставим бомбу, то текущая ячейка это -30
        if (isAct) {
            step = newWay.get(board.getBomberman());
            step.expectedCost = step.expectedCost - 300;
            System.out.println("step:" + step.toString());
            newWay.put(board.getBomberman(), step);
        }
        //Arseniy: найдём самые вкусные точки
        for (Map.Entry<Point, Step> entry: newWay.entrySet()) {
            step = entry.getValue();
            if (step.expectedCost > bestCost) {
                bestCost = step.expectedCost;
            }
        }

        System.out.println("bestCost:" + bestCost);

        ArrayList<Point> bestPoints = new ArrayList<>();
        for(Map.Entry<Point, Step> entry: newWay.entrySet()){
            step = entry.getValue();
            if (step.expectedCost == bestCost) {
                bestPoints.add(entry.getKey());
            }
        }
        System.out.println("bestPoints:" + bestPoints.toString());
        //Arseniy: чтобы на следующем ходу понимать, пришли мы в эту лучшую точку?
        if (bestCost > 0) {
            bestPointsGlob = new ArrayList<>(bestPoints);
        } else {
            for(int i = bestPointsGlob.size() - 1; i >= 0; i-- ) {
                bestPointsGlob.remove(i);
            }
        }
        //Arseniy: пройдём от вкусных точек на нашему бомберу и сохраним первый шаг в сторону вкусной точки
        boolean goOn;
        Point previousPoint;
        ArrayList<Point> firstSteps = new ArrayList<>();
        for(Point point : bestPoints){
            goOn = true;
            step = newWay.get(point);
            previousPoint = point;
            while (goOn){
                System.out.println("point,previousPoint:" + point.toString() + ";" + previousPoint + ";" + step.previousPoint.toString());
                if (step.previousPoint.itsMe(board.getBomberman())) {
                    if (!firstSteps.contains(previousPoint)) {
                        firstSteps.add(previousPoint);
                    }
                    goOn = false;
                } else {
                    previousPoint = new PointImpl(step.previousPoint);
                    step = newWay.get(step.previousPoint);
                }
            }
        }

        System.out.println("firstSteps:" + firstSteps);
        firstSteps.retainAll(theWay);

        //Arseniy:Вычисляем безопасное направление
        ArrayList<Point> theWaySafe = new ArrayList<>(firstSteps);

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

            ArrayList<Point> theWayLessSafe = new ArrayList<>(firstSteps);

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

                theWayLessSafe = new ArrayList<>(firstSteps);
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

                    theWayLessSafe = new ArrayList<>(firstSteps);
                    theWayLessSafe.removeAll(fBlasts1);
                    theWayLessSafe.removeAll(fMeatChoppers);
                    theWayLessSafe.removeAll(fBlasts2);
                    theWayLessSafe.removeAll(fBlasts3);
                    System.out.println("lessSafe-blast3:" + theWayLessSafe.toString());
                    if (theWayLessSafe.size() > 0) {
                        finalWay = new ArrayList<>(theWayLessSafe);
                        System.out.println("finalWayBl3:" + finalWay.toString());
                    } else {

                        theWayLessSafe = new ArrayList<>(firstSteps);
                        theWayLessSafe.removeAll(fBlasts1);
                        theWayLessSafe.removeAll(fMeatChoppers);
                        theWayLessSafe.removeAll(fBlasts2);
                        System.out.println("lessSafe-blast2:" + theWayLessSafe.toString());
                        if (theWayLessSafe.size() > 0) {
                            finalWay = new ArrayList<>(theWayLessSafe);
                            System.out.println("finalWayBl2:" + finalWay.toString());
                        } else {

                            theWayLessSafe = new ArrayList<>(firstSteps);
                            theWayLessSafe.removeAll(fBlasts1);
                            theWayLessSafe.removeAll(fMeatChoppers);
                            System.out.println("lessSafe-fChoppers:" + theWayLessSafe.toString());
                            if (theWayLessSafe.size() > 0) {
                                finalWay = new ArrayList<>(theWayLessSafe);
                                System.out.println("finalWayfCHp:" + finalWay.toString());
                            } else {
                                theWayLessSafe = new ArrayList<>(firstSteps);
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
            for (int i = 1; i < blastRadius; i++) {
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

        Collections.shuffle(finalWay);

        if (finalWay.size() <= 0) {
            finalWay.add(board.getBomberman());
        }

        System.out.println(finalWay.get(0).toString());
        System.out.println(finalWay.get(0).relative(board.getBomberman()).toString());

        String move;
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

    private void addNewStep( HashMap<Point, Step> newWay,
                                    Collection<Point> currentPoints,
                                    Point previousPoint,
                                    int depth) {

        int expectedCost = 0;
        ArrayList<Point> newPoints;
        HashMap <Point, ArrayList<Point>> allNextSteps = new HashMap<>();
        Step newStep;

        depth++;

        for (Point currentPoint : currentPoints) {
            //Arseniy: не считаем второй раз для одной и той же точки
            if (!newWay.containsKey(currentPoint)) {
                expectedCost = expectedCost(currentPoint, depth);
                newPoints = new ArrayList<>(newDirections(currentPoint, depth));
                newPoints.remove(previousPoint);
                //Arseniy: удаляем точки, для которых уже был расчёт
                for(int i = newPoints.size() - 1; i >= 0; i--) {
                    if (newWay.containsKey(newPoints.get(i))) {
                        newPoints.remove(i);
                    }
                }
                newStep = new Step(previousPoint, expectedCost, newPoints);
                newWay.put(currentPoint, newStep);

                //Arseniy: соберём все точки вместе, чтобы по всем сразу отдельно сделать просчёт
                allNextSteps.put(currentPoint, newPoints);
                System.out.println("currentPoint,newPoints:" + currentPoint + ";" + newPoints.toString());
            }
        }

        if (depth < 5) {
            for(Map.Entry<Point, ArrayList<Point>> entry : allNextSteps.entrySet()) {
                addNewStep(newWay, entry.getValue(), entry.getKey(), depth);
            }
        }

    }

    //Arseniy: сколько я могу получить, если приду в эту точку (и поставлю бомбу)
    private int expectedCost (Point currentPoint,
                              int depth) {
        boolean debug = true;
        int expectedCost = 0;

        if (debug) {
            System.out.println("expectedCost:" + currentPoint + ";" + expectedCost);
        }

        Collection<Point> walls         = board.getWalls();
        Collection<Point> dWalls        = board.getDestroyableWalls();
        Collection<Point> perks         = board.getPerks();
        Collection<Point> fMeatChoppers = board.getFutureMeatChoppers();
        Collection<Point> bomb1         = board.get(Elements.BOMB_TIMER_1);
        Collection<Point> bomb2         = board.get(Elements.BOMB_TIMER_2);
        Collection<Point> bomb3         = board.get(Elements.BOMB_TIMER_3);
        Collection<Point> bomb4         = board.get(Elements.BOMB_TIMER_4);
        Collection<Point> bomb5         = board.get(Elements.BOMB_TIMER_5);
        bomb5.addAll(board.get(Elements.BOMB_BOMBERMAN));
        bomb5.addAll(board.get(Elements.OTHER_BOMB_BOMBERMAN));

        Collection<Point> fBlasts1      = board.getFutureBlastsByCollection(bomb1);
        Collection<Point> fBlasts2      = board.getFutureBlastsByCollection(bomb2);
        Collection<Point> fBlasts3      = board.getFutureBlastsByCollection(bomb3);
        Collection<Point> fBlasts4      = board.getFutureBlastsByCollection(bomb4);
        Collection<Point> fBlasts5      = board.getFutureBlastsByCollection(bomb5);

        //Arseniy: перки дают очки, но далеко за ними идти не стоит
        for (Point perk : perks) {
            if (currentPoint.itsMe(perk) & depth <= 10) {
                expectedCost = expectedCost + 100 - depth * 10;
                if (debug) {
                    System.out.println("expectedCost-perk:" + currentPoint + ";" + expectedCost);
                }
            }
        }

        //Arseniy: в точки, куда могут придти чопперы идти не стоит
        if (depth <= 3 & fMeatChoppers.contains(currentPoint)) {
            expectedCost = expectedCost - 150;
            if (debug) {
                System.out.println("expectedCost-fChopper:" + currentPoint + ";" + expectedCost);
            }
        }

        //Arseniy: во взрывы тоже не стоит идти
        if (depth <= 5) {
            if (fBlasts1.contains(currentPoint)) {
                expectedCost = expectedCost - 300;

            }
            if (fBlasts2.contains(currentPoint)) {
                expectedCost = expectedCost - 299;

            }
            if (fBlasts3.contains(currentPoint)) {
                expectedCost = expectedCost - 298;

            }
            if (fBlasts4.contains(currentPoint)) {
                expectedCost = expectedCost - 297;

            }
            if (fBlasts5.contains(currentPoint)) {
                expectedCost = expectedCost - 296;

            }
        }

        //Arseniy: расчёты, если я поставлю бомбу
        ArrayList<Point> blasts = new ArrayList<>();
        blasts.add(currentPoint);

        PointImpl blast;
        PointImpl prevBlast;

        for (int i = 1 ; i <=3 ; i++) {
            blast = new PointImpl(currentPoint.getX() - i, currentPoint.getY());
            prevBlast = new PointImpl(currentPoint.getX() - i + 1, currentPoint.getY());
            if (blasts.contains(prevBlast)) {
                expectedCost = expectedCost + atBlast(blast, depth);
                if (!walls.contains(blast) & !dWalls.contains(blast)) {
                    blasts.add(blast);
                }
            }
            blast = new PointImpl(currentPoint.getX() + i, currentPoint.getY());
            prevBlast = new PointImpl(currentPoint.getX() + i - 1, currentPoint.getY());
            if (blasts.contains(prevBlast)) {
                expectedCost = expectedCost + atBlast(blast, depth);
                if (!walls.contains(blast) & !dWalls.contains(blast)) {
                    blasts.add(blast);
                }
            }
            blast = new PointImpl(currentPoint.getX(), currentPoint.getY() - i);
            prevBlast = new PointImpl(currentPoint.getX(), currentPoint.getY() - i + 1);
            if (blasts.contains(prevBlast)) {
                expectedCost = expectedCost + atBlast(blast, depth);
                if (!walls.contains(blast) & !dWalls.contains(blast)) {
                    blasts.add(blast);
                }
            }
            blast = new PointImpl(currentPoint.getX(), currentPoint.getY() + i);
            prevBlast = new PointImpl(currentPoint.getX(), currentPoint.getY() + i - 1);
            if (blasts.contains(prevBlast)) {
                expectedCost = expectedCost + atBlast(blast, depth);
                if (!walls.contains(blast) & !dWalls.contains(blast)) {
                    blasts.add(blast);
                }
            }
        }
        if (debug) {
            System.out.println("exp-cost-fblast:" + blasts.toString());
        }
        return expectedCost;
    }

    //Arseniy: сколько получу, если взорву эту точку
    private int atBlast (Point point,
                         int depth) {

        boolean debug = true;

        Collection<Point> oBombers      = board.getOtherBombermans();
        Collection<Point> perks         = board.getPerks();
        Collection<Point> meatChoppers  = board.getMeatChoppers();
        Collection<Point> dWalls        = board.getDestroyableWalls();

        int cost = 0;
        //Arseniy: взрыв перка = всегда дурной чоппер = смерть
        if (perks.contains(point)) {
            cost = cost - 300;
            if (debug) {
                System.out.println("expectedCost-perk-destr:" + point + ";" + cost);
            }
        }
        //Arseniy: стены приносят очки, но чем они дальше, тем меньше дают
        for (Point dWall : dWalls) {
            if (point.itsMe(dWall)) {
                if (depth <= 2) {
                    cost = cost + 30 - depth;
                    if (debug) {
                        System.out.println("expectedCost-dwall:" + point + ";" + cost);
                    }
                }
            }
        }
        //todo: не считать одного чоппера в разных точка взрыва
        //Arseniy: чопперы - тоже самое, если они рядом от взрыва, и потеницальная бомба недалеко, то очки, наверное, получим
        for (Point chopper: meatChoppers) {
            if (Math.abs(point.getX() - chopper.getX()) + Math.abs(point.getY() - chopper.getY()) <= 0) {
                if (depth <= 3) {
                    cost = cost + 20 - depth;
                    if (debug) {
                        System.out.println("expectedCost-chopper:" + point + ";" + cost);
                    }
                }
            }
        }
        //todo: не считать одного игрока в разных точка взрыва
        //Arseniy: игрок - тоже самое, что и чопперы, только очков побольше и скорее стоят, чем двигаются
        for (Point oBomber: oBombers) {
            if (Math.abs(point.getX() - oBomber.getX()) + Math.abs(point.getY() - oBomber.getY()) <= 0) {
                if (depth <= 10) {
                    cost = cost + 100 - depth * 10;
                    if (debug) {
                        System.out.println("expectedCost-player:" + point + ";" + cost);
                    }
                }
            }
        }
        return cost;
    }

    private ArrayList<Point> newDirections (Point currentPoint,
                                            int depth) {
        boolean debug = false;
        ArrayList<Point> newDirections = new ArrayList<>();

        if (debug) {
            System.out.println("meth-newDir:" + currentPoint + ";" + depth);
        }
        newDirections.add(new PointImpl(currentPoint.getX() + 1, currentPoint.getY()));
        newDirections.add(new PointImpl(currentPoint.getX() - 1, currentPoint.getY()));
        newDirections.add(new PointImpl(currentPoint.getX(), currentPoint.getY() + 1));
        newDirections.add(new PointImpl(currentPoint.getX(), currentPoint.getY() - 1));

        Collection<Point> walls         = board.getWalls();
        Collection<Point> meatChoppers  = board.getMeatChoppers();
        Collection<Point> fMeatChoppers = board.getFutureMeatChoppers();
        Collection<Point> dWalls        = board.getDestroyableWalls();
        Collection<Point> oBombers      = board.getOtherBombermans();

        Collection<Point> bombs         = board.getBombs();
        Collection<Point> fBlasts       = board.getFutureBlasts();

        Collection<Point> bomb1         = board.get(Elements.BOMB_TIMER_1);
        Collection<Point> bomb2         = board.get(Elements.BOMB_TIMER_2);
        Collection<Point> bomb3         = board.get(Elements.BOMB_TIMER_3);
        Collection<Point> bomb4         = board.get(Elements.BOMB_TIMER_4);
        Collection<Point> bomb5         = board.get(Elements.BOMB_TIMER_5);
        bomb5.addAll(board.get(Elements.BOMB_BOMBERMAN));
        bomb5.addAll(board.get(Elements.OTHER_BOMB_BOMBERMAN));

        Collection<Point> fBlasts1      = board.getFutureBlastsByCollection(bomb1);
        Collection<Point> fBlasts2      = board.getFutureBlastsByCollection(bomb2);
        Collection<Point> fBlasts3      = board.getFutureBlastsByCollection(bomb3);
        Collection<Point> fBlasts4      = board.getFutureBlastsByCollection(bomb4);
        Collection<Point> fBlasts5      = board.getFutureBlastsByCollection(bomb5);

        if (debug) {
            System.out.println("bomb1-newdir:" + depth + ";" + bomb1.toString());
            System.out.println("bomb2-newdir:" + depth + ";" + bomb2.toString());
            System.out.println("bomb3-newdir:" + depth + ";" + bomb3.toString());
            System.out.println("bomb4-newdir:" + depth + ";" + bomb4.toString());
            System.out.println("bomb5-newdir:" + depth + ";" + bomb5.toString());
            System.out.println("fBlasts1-newdir:" + depth + ";" + fBlasts1.toString());
            System.out.println("fBlasts2-newdir:" + depth + ";" + fBlasts2.toString());
            System.out.println("fBlasts3-newdir:" + depth + ";" + fBlasts3.toString());
            System.out.println("fBlasts4-newdir:" + depth + ";" + fBlasts4.toString());
            System.out.println("fBlasts5-newdir:" + depth + ";" + fBlasts5.toString());
        }

        newDirections.removeAll(walls);
        if (debug) {
            System.out.println("-walls-newdir:" + depth + ";" + newDirections.toString());
        }
        newDirections.removeAll(dWalls);
        if (debug) {
            System.out.println("-dWalls-newdir:" + depth + ";" + newDirections.toString());
        }

        if (depth <= 2) {
            newDirections.removeAll(oBombers);
            if (debug) {
                System.out.println("-oBombers-newdir:" + depth + ";" + newDirections.toString());
            }
        }
        if (depth <= 2) {
            newDirections.removeAll(meatChoppers);
            if (debug) {
                System.out.println("-choppers-newdir:" + depth + ";" + newDirections.toString());
            }
        }
        if (depth <= 2) {
            newDirections.removeAll(fMeatChoppers);
            if (debug) {
                System.out.println("-fChoppers-newdir:" + depth + ";" + newDirections.toString());
            }
        }

        if (depth == 1) {
            newDirections.removeAll(bomb1);
            newDirections.removeAll(bomb2);
            newDirections.removeAll(bomb3);
            newDirections.removeAll(bomb4);
            newDirections.removeAll(bomb5);
            newDirections.removeAll(fBlasts1);
            if (debug) {
                System.out.println("-blast1-new-dir:" + depth + ";" + newDirections.toString());
            }
        }

        if (depth == 2) {
            newDirections.removeAll(bomb2);
            newDirections.removeAll(bomb3);
            newDirections.removeAll(bomb4);
            newDirections.removeAll(bomb5);
            newDirections.removeAll(fBlasts2);
            if (debug) {
                System.out.println("-blast2-new-dir:" + depth + ";" + newDirections.toString());
            }
        }

        if (depth == 3) {
            newDirections.removeAll(bomb3);
            newDirections.removeAll(bomb4);
            newDirections.removeAll(bomb5);
            newDirections.removeAll(fBlasts3);
            if (debug) {
                System.out.println("-blast3-new-dir:" + depth + ";" + newDirections.toString());
            }
        }

        if (depth == 4) {
            newDirections.removeAll(bomb4);
            newDirections.removeAll(bomb5);
            newDirections.removeAll(fBlasts4);
            if (debug) {
                System.out.println("-blast4-new-dir:" + depth + ";" + newDirections.toString());
            }
        }

        if (depth == 5) {
            newDirections.removeAll(bomb5);
            newDirections.removeAll(fBlasts5);
            if (debug) {
                System.out.println("-blast5-new-dir:" + depth + ";" + newDirections.toString());
            }
        }

        return newDirections;
    }


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
