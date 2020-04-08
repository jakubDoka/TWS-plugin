package example;

import arc.util.Timer;

public class Request implements Interruptible {
    int time;
    
    boolean stoppable=true;
    Timer.Task task;
    Timer.Task terminateTask;
    Requester requester;
    Package aPackage;


    Timer.Task countdown=new Timer.Task() {
        @Override
        public void run() {
            time--;
            if(time==0){
                countdown.cancel();
            }
        }
    };

    public Request(int time, Timer.Task task, Requester requester, Package aPackage,boolean stoppable){
        this.task=task;
        this.time=time;
        this.requester=requester;
        this.aPackage=aPackage;
        this.stoppable=stoppable;
        Timer.schedule(task,time);
        Timer.schedule(countdown, 1, 1);
        terminateTask=Timer.schedule(this::terminate,time);
    }

    private void terminate(){
        requester.getRequests().remove(this);
    }

    @Override
    public void interrupt() {
        if(!stoppable){
            return;
        }
        requester.fail(aPackage.object,aPackage.amount);
        task.cancel();
        terminateTask.cancel();

    }
}
