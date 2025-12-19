public class Request {
    private final int sourceFloor;
    private final int targetFloor;
    private final Direction direction;
    private final RequestType type;
    private final int elevatorId;
    
    public Request(int sourceFloor, int targetFloor, Direction direction, RequestType type) {
        this.sourceFloor = sourceFloor;
        this.targetFloor = targetFloor;
        this.direction = direction;
        this.type = type;
        this.elevatorId = -1;
    }
    
    public Request(int targetFloor, int elevatorId) {
        this.sourceFloor = -1;
        this.targetFloor = targetFloor;
        this.direction = Direction.NONE;
        this.type = RequestType.INTERNAL;
        this.elevatorId = elevatorId;
    }
    
    public int getSourceFloor() { return sourceFloor; }
    public int getTargetFloor() { return targetFloor; }
    public Direction getDirection() { return direction; }
    public RequestType getType() { return type; }
    public int getElevatorId() { return elevatorId; }
}