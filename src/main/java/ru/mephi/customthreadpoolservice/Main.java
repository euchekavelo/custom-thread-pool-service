package ru.mephi.customthreadpoolservice;

import ru.mephi.customthreadpoolservice.custom.CustomThreadPool;
import ru.mephi.customthreadpoolservice.custom.SimulationTask;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        //Демонстрационная задача
        SimulationTask task = new SimulationTask();

        //Демонстрация работы пула при оптимальном количестве задач
        CustomThreadPool optimalTaskPool = new CustomThreadPool(2, 4, 2, 5,
                TimeUnit.SECONDS, 5);

        for (int i = 0; i < 10; i++) {
            optimalTaskPool.execute(task);
        }
        optimalTaskPool.shutdown();
        //---------------------------------------------------------

        //Демонстрация работы пула при слишком большом количестве задач (производится отклонение задач сверх емкости очередей)
        CustomThreadPool overTaskPool = new CustomThreadPool(2, 4, 2, 5,
                TimeUnit.SECONDS, 5);
        for (int i = 0; i < 100; i++) {
            overTaskPool.execute(new SimulationTask());
        }
        overTaskPool.shutdown();
        //---------------------------------------------------------
    }
}