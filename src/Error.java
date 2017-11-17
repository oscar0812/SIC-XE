public class Error {
    private String description;
    private int index;
    Error(String description, int index){
        this.description = description;
        this.index = index;
    }

    public String getDescription() {
        return description;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj.getClass()!=getClass())
            return false;
        Error e = ((Error)obj);
        return e.getDescription().equals(getDescription()) && e.getIndex() == getIndex();
    }
}
