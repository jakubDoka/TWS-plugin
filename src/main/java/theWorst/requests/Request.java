package theWorst.requests;

import arc.util.Timer;
import theWorst.Package;

public class Request {
    int time;

    boolean stoppable;
    Timer.Task task;
    Timer.Task terminateTask;
    Requester requester;
    public Package aPackage;


    Timer.Task countdown = new Timer.Task() {
        @Override
        public void run() {
            time--;
            if (time == 0) {
                countdown.cancel();
            }
        }
    };

    public Request(int time, Timer.Task task, Requester requester, Package aPackage, boolean stoppable) {
        this.task = task;
        this.time = time;
        this.requester = requester;
        this.aPackage = aPackage;
        this.stoppable = stoppable;

        Timer.schedule(task, time);
        Timer.schedule(countdown, 0, 1);
        terminateTask = Timer.schedule(this::terminate, time);
    }

    private void terminate() {
        requester.getRequests().remove(this);
    }

    public void interrupt() {
        if (!stoppable) {
            return;
        }
        requester.fail(aPackage.object, aPackage.amount);
        task.cancel();
        terminateTask.cancel();

    }
}
