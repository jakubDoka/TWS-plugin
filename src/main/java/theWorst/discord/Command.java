package theWorst.discord;

public abstract class Command {
    public String name;

    public String description = ": no description provided.";

    public String argStruct="noArgs";

    public int maxArgs=0,minArgs=0;

    public Command(String name,String argStruct) {
        this.name=name;
        this.argStruct=argStruct;
        resolveArgStruct();
    }

    public Command(String name) {
        this.name=name;
    }

    public void resolveArgStruct() {
        if(argStruct == null) return;
        String[] args = argStruct.split(" ");
        for(String arg:args){
            if(arg.startsWith("<")){
                maxArgs++;
                minArgs++;
            } else if(arg.startsWith("[")){
                maxArgs++;
            }
            if(arg.contains("...")){
                maxArgs=10000;
                break;
            }
        }
    }

    public abstract void run(CommandContext ctx);

    public boolean hasPerm(CommandContext ctx){return true;}
}
