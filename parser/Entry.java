package parser;

public class Entry {
    //状态表示 错误 接受 移入 归约
    public final static int
            ERROR = 0, ACCEPT = 1,
            SHIFT = 2, REDUCE = 3,
            GOTO = 5;
    private int state;//接下来的状态
    private int action;//要执行的动作

    public Entry(int state, int action) {
        this.state = state;
        this.action = action;
    }

    @Override
    public String toString() {
        return "Entry{" +
                "state=" + state +
                ", action=" + action +
                '}';
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }
}
