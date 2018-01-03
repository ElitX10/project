/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.audio.AudioNode;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.shape.Cylinder;
import java.util.Random;
import mygame.Globals.*;

/**
 *
 * @author ThomasLeScolan
 */
public class Globals {
    // variable for setting the server and the clients :
    public static final String NAME = "Lab3";
    public static final String DEFAULT_SERVER = "Localhost"; //"130.240.157.44";
    public static final int VERSION = 1;
    public static final int DEFAULT_PORT = 6143;
    
    // register all message types there are
    public static void initialiseSerializables() {
        Serializer.registerClass(RandomEventMessage.class);
        Serializer.registerClass(DrumPositionMessage.class);
        Serializer.registerClass(CarPositionMessage.class);
    }
    
    public static void createScene(Node GameNode, SimpleApplication myApp, BulletAppState bulletAppState){
        
        // create the road :
        Spatial gameLevel = myApp.getAssetManager().loadModel("Scenes/Road.j3o");
        gameLevel.scale(3);
        Node road = new Node("landscape");
        road.setLocalTranslation(-50,0,-80);
        road.attachChild(gameLevel);
        GameNode.attachChild(road);
        
        CollisionShape sceneShape2 = CollisionShapeFactory.createMeshShape(road);
        RigidBodyControl landscape2 = new RigidBodyControl(sceneShape2, 0);
        road.addControl(landscape2);
        bulletAppState.getPhysicsSpace().add(landscape2);
        
        // create the sky : 
        // TODO !!
        // add some trees and rocks :
        // TODO !!
        // add light : 
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        GameNode.addLight(dl);
    }
    
    @Serializable
    public static class RandomEventMessage extends AbstractMessage{
        
    }
    
    @Serializable
    public static class DrumPositionMessage extends AbstractMessage{
        private float X_Pos[];
        private float Y_Pos[];
        private float Z_Pos[];
        private float rotation[][];
        
        public DrumPositionMessage(){
            
        }
        
        public DrumPositionMessage(float X[], float Y[], float Z[], float rot[][]){
            X_Pos = X;
            Y_Pos = Y;
            Z_Pos = Z;
            rotation = rot;
        }
        
        public float[] getX(){
            return X_Pos;
        }
        
        public float[] getY(){
            return Y_Pos;
        }
        
        public float[] getZ(){
            return Z_Pos;
        }
        
        public float[][] getRotation(){
            return rotation;
        }
    }
    
    @Serializable
    public static class CarPositionMessage extends AbstractMessage{
        private float X_Pos[];
        private float Y_Pos[];
        private float Z_Pos[];
        private float rotation[][];
        
        public CarPositionMessage(){
            
        }
        
        public CarPositionMessage(float X[], float Y[], float Z[], float rot[][]){
            X_Pos = X;
            Y_Pos = Y;
            Z_Pos = Z;
            rotation = rot;
        }
        
        public float[] getX(){
            return X_Pos;
        }
        
        public float[] getY(){
            return Y_Pos;
        }
        
        public float[] getZ(){
            return Z_Pos;
        }
        
        public float[][] getRotation(){
            return rotation;
        }
    }
    
    @Serializable
    public static class CarParameterMessage extends AbstractMessage{
        private float accelerationValue;
        private float steeringValue;
        private float brakingValue;
        private int playerID;
        private boolean stopSound;
        private boolean startSound;
        private boolean accelerationSound; 
                
        
        public CarParameterMessage(){
            
        }
        
        public CarParameterMessage(float acceleration, float steer, float brake, boolean stop, boolean start, boolean accelerationS, int id){
            accelerationValue = acceleration;
            steeringValue = steer;
            brakingValue = brake;
            playerID = id;
            stopSound = stop;
            startSound = start;
            accelerationSound = accelerationS;
        }
        
        public float getAcceleration(){
            return accelerationValue;
        }
        
        public float getSteer(){
            return steeringValue;
        }
        
        public float getBrake(){
            return brakingValue;
        }
        
        public int getID(){
            return playerID;
        }
        
        public boolean getStop(){
            return stopSound;
        }
        
        public boolean getStart(){
            return startSound;
        }
        
        public boolean getAccelerationSound(){
            return accelerationSound;
        }
    }
}

//-------------------------------------------------GAME---------------------------------------------------------------------------------------
class Game extends BaseAppState{

    @Override
    protected void initialize(Application app) {
        
    }

    @Override
    public void update(float tpf) {

    }
    
    @Override
    protected void cleanup(Application app) {
        
    }

    @Override
    protected void onEnable() {
        
    }

    @Override
    protected void onDisable() {
        
    }
    
}

//-------------------------------------------------PLAYER---------------------------------------------------------------------------------------
class Player extends BaseAppState{
    // initial position :
    private final int X_Tab[] = {110, 110, 126, 126};
    private final float Z_Tab[] = {-1.5f, -13.5f, -1.5f, -13.5f};
    private final float Y_Initial = 1;
    
    private Node carNode;
    private VehicleControl player;
    private float wheelRadius;
    private final SimpleApplication myApp;
    private AudioNode accelerationSoundNode;
    private AudioNode startSoundNode;
    private AudioNode stopSoundNode;
    private CameraNode camNode;
    private final Node NODE_GAME;
    private final BulletAppState myBulletAppState;
    private final int ID;
    private int hostNumber = -1;
    private static int numberOfPlayer = 0;
    private boolean isConnected = true;
    private final boolean isControlled;
    
    public Player(SimpleApplication app, Node gameNode, BulletAppState bulletAppState, boolean control){
        myApp = app;
        NODE_GAME = gameNode;
        myBulletAppState = bulletAppState;
        
        // increase the number of player every time we create one player  
        numberOfPlayer++;
        
        // give an id to every player to separate input later and for displaying the score on the sceen for each player :
        this.ID = numberOfPlayer;
        
        isControlled = control;
    }  
    
    public Player(SimpleApplication app, Node gameNode, BulletAppState bulletAppState, int hostNum, boolean control){
        myApp = app;
        NODE_GAME = gameNode;
        myBulletAppState = bulletAppState;
        
        // increase the number of player every time we create one player  
        numberOfPlayer++;
        
        // give an id to every player to separate input later and for displaying the score on the sceen for each player :
        this.ID = numberOfPlayer;
        
        // set host number for server :
        hostNumber = hostNum;
        
        isControlled = control;
    }
    
    public static void resetNumberOfPlayer(){
        numberOfPlayer = 0;
    }
    
    @Override
    protected void initialize(Application app) {
        buildPlayer(isControlled);
    }

    @Override
    public void update(float tpf) {

    }
    
    @Override
    protected void cleanup(Application app) {
        
    }

    @Override
    protected void onEnable() {        
        NODE_GAME.attachChild(carNode);
        getPhysicsSpace().add(player);
        player.setPhysicsLocation(new Vector3f(X_Tab[ID -1], Y_Initial, Z_Tab[ID -1]));
        player.setPhysicsLocation(new Vector3f(40, 3, 350)); 
        player.setPhysicsRotation(new Matrix3f( 0, 0, 1,
                                                0, 1, 0, 
                                                -1, 0, 0));
    }

    @Override
    protected void onDisable() {
        NODE_GAME.detachChild(carNode);
        getPhysicsSpace().remove(player);
        isConnected = false;
    }
    
    private void buildPlayer(boolean isControlled) {
        float stiffness = 120.0f;//200=f1 car
        float compValue = 0.2f; //(lower than damp!)
        float dampValue = 0.3f;
        final float mass = 400;
        
        //Load model and get chassis Geometry
        carNode = (Node)myApp.getAssetManager().loadModel("Models/Ferrari/Car.scene");
        carNode.setShadowMode(RenderQueue.ShadowMode.Cast);
        Geometry chasis = findGeom(carNode, "Car");
        BoundingBox box = (BoundingBox) chasis.getModelBound();
        
        //Create a hull collision shape for the chassis
        CollisionShape carHull = CollisionShapeFactory.createDynamicMeshShape(chasis);
        
        //Create a vehicle control
        player = new VehicleControl(carHull, mass);
        carNode.addControl(player);
        
        //Setting default values for wheels
        player.setSuspensionCompression(compValue * 2.0f * FastMath.sqrt(stiffness));
        player.setSuspensionDamping(dampValue * 2.0f * FastMath.sqrt(stiffness));
        player.setSuspensionStiffness(stiffness);
        player.setMaxSuspensionForce(10000);
        
        //Create four wheels and add them at their locations
        //note that our fancy car actually goes backwards..
        Vector3f wheelDirection = new Vector3f(0, -1, 0);
        Vector3f wheelAxle = new Vector3f(-1, 0, 0);

        Geometry wheel_fr = findGeom(carNode, "WheelFrontRight");
        wheel_fr.center();
        box = (BoundingBox) wheel_fr.getModelBound();
        wheelRadius = box.getYExtent();
        float back_wheel_h = (wheelRadius * 1.7f) - 1f;
        float front_wheel_h = (wheelRadius * 1.9f) - 1f;
        player.addWheel(wheel_fr.getParent(), box.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, true);

        Geometry wheel_fl = findGeom(carNode, "WheelFrontLeft");
        wheel_fl.center();
        box = (BoundingBox) wheel_fl.getModelBound();
        player.addWheel(wheel_fl.getParent(), box.getCenter().add(0, -front_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, true);

        Geometry wheel_br = findGeom(carNode, "WheelBackRight");
        wheel_br.center();
        box = (BoundingBox) wheel_br.getModelBound();
        player.addWheel(wheel_br.getParent(), box.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, false);

        Geometry wheel_bl = findGeom(carNode, "WheelBackLeft");
        wheel_bl.center();
        box = (BoundingBox) wheel_bl.getModelBound();
        player.addWheel(wheel_bl.getParent(), box.getCenter().add(0, -back_wheel_h, 0),
                wheelDirection, wheelAxle, 0.2f, wheelRadius, false);

        player.getWheel(2).setFrictionSlip(9);
        player.getWheel(3).setFrictionSlip(9);
        
        if(isControlled){
            // camera following the car :
            myApp.getFlyByCamera().setEnabled(false);
    //        flyCam.setEnabled(false);
            camNode = new CameraNode("Camera Node", myApp.getCamera());
            camNode.setControlDir(ControlDirection.SpatialToCamera);
            carNode.attachChild(camNode);
            camNode.setLocalTranslation(new Vector3f(0, 5, 15));
            camNode.lookAt(carNode.getLocalTranslation(), Vector3f.UNIT_Y);
        }
        
        
        // car sound :
        AudioNode engineNoiseNode = new AudioNode(myApp.getAssetManager(), "Sounds/engineNoise.ogg");
        engineNoiseNode.setPositional(true);
        engineNoiseNode.setLooping(true);
        engineNoiseNode.setVolume(0.1f);
        carNode.attachChild(engineNoiseNode);
        if (myApp instanceof ServerMain){
            
        }else{
            engineNoiseNode.play();
        }        
        
        accelerationSoundNode = new AudioNode(myApp.getAssetManager(), "Sounds/acceleration.ogg");
        accelerationSoundNode.setPositional(true);
        accelerationSoundNode.setLooping(false);
        accelerationSoundNode.setVolume(0.3f);
        carNode.attachChild(accelerationSoundNode);
        
        startSoundNode = new AudioNode(myApp.getAssetManager(), "Sounds/Start.ogg");
        startSoundNode.setPositional(true);
        startSoundNode.setLooping(false);
        startSoundNode.setVolume(0.3f);
        carNode.attachChild(startSoundNode);
        
        stopSoundNode = new AudioNode(myApp.getAssetManager(), "Sounds/stoping.ogg");
        stopSoundNode.setPositional(true);
        stopSoundNode.setLooping(false);
        stopSoundNode.setVolume(0.3f);
        carNode.attachChild(stopSoundNode);        
    }
    
    private Geometry findGeom(Spatial spatial, String name) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (int i = 0; i < node.getQuantity(); i++) {
                Spatial child = node.getChild(i);
                Geometry result = findGeom(child, name);
                if (result != null) {
                    return result;
                }
            }
        } else if (spatial instanceof Geometry) {
            if (spatial.getName().startsWith(name)) {
                return (Geometry) spatial;
            }
        }
        return null;
    }
    
    private PhysicsSpace getPhysicsSpace() {
        return myBulletAppState.getPhysicsSpace();
    }

    public float getCurrentVehicleSpeedKmHour(){
        return player.getCurrentVehicleSpeedKmHour();
    }
    
    public void accelerate(float accelerationValue) {
        player.accelerate(accelerationValue);
    }

    public void steer(float steeringValue) {
        player.steer(steeringValue);
    }

    public void setPhysicsLocation(Vector3f newLocation) {
        player.setPhysicsLocation(newLocation);
    }

    public void setPhysicsRotation(Matrix3f newRotation) {
        player.setPhysicsRotation(newRotation);
    }

    public void setAngularVelocity(Vector3f newAngularVelocity) {
        player.setAngularVelocity(newAngularVelocity);
    }

    public void setLinearVelocity(Vector3f newLinearVelocity) {
        player.setLinearVelocity(newLinearVelocity);
    }

    public void resetSuspension() {
        player.resetSuspension();
    }

    public void brake(float f) {
        player.brake(f);
    }
    
    public void playStartSoundNode() {
        startSoundNode.play();
    }

    public void playAccelerationSoundNode() {
        accelerationSoundNode.play(); 
    }

    public void stopStopSoundNode() {
        stopSoundNode.stop();
    }

    void stopAccelerationSoundNode() {
        accelerationSoundNode.stop();
    }

    void stopStartSoundNode() {
        startSoundNode.stop();
    }

    void playStopSoundNode() {
        stopSoundNode.play();
    }
    
    public int getHostNum(){
        return hostNumber;
    }
    
    public Vector3f getPosition(){
        return player.getPhysicsLocation();
    }
    
    public Matrix3f getRotation() {
        return player.getPhysicsRotationMatrix();
    }
    
    public void setPosition(float X, float Y, float Z){
        player.setPhysicsLocation(new Vector3f(X, Y, Z));
    }
}

//-------------------------------------------------TRUCK---------------------------------------------------------------------------------------
class Truck extends BaseAppState{
    private float X_Position = 15;
    private float Y_Position = 2f;
    private float Z_Position = 342;
//    private final int X_GlobalPosition = 15;
//    private final int Y_GlobalPosition = 0;
//    private final int Z_GlobalPosition = 320;
    private final SimpleApplication myApp;
    private final Node GameNode;
    private Spatial truck;
    private Node truckNode;
//    private int X_Path[] = {0,0,0,0,0,0,0,0,-40,-40,-40,-40,-40,-40,-40,-40,0};
//    private int Y_Path[] = {0,3,3,0,0,3,3,0,0,3,3,0,0,3,3,0,0};
//    private int Z_Path[] = {-45,-35,-15,-10,5,10,30,40,40,30,10,5,-10,-15,-35,-45,-45};
//    private int pathIndex = 0;
//    private float globalSpeed = 8;
//    private float X_Speed;
//    private float Y_Speed;
//    private float Z_Speed;
//    private double distance;
//    private float timeToNextStep;
    private final Random myRand = new Random();
    private final int randomTimerDelay = myRand.nextInt((10 - 8) + 1) + 8; //myRand.nextInt((60 - 25) + 1) + 25
    private float randomTimer = 0;
//    private boolean move = false;
    private final BulletAppState myBulletAppState;
//    private RigidBodyControl rigidTruck1;
    private RigidBodyControl[] rigidOilDrum = new RigidBodyControl[3];
    private boolean drumOnTheTruck = true;
    
    public Truck(SimpleApplication app, Node gameNode, BulletAppState bulletAppState){
        myApp = app;
        GameNode = gameNode;
        myBulletAppState = bulletAppState;
    }
    
    @Override
    protected void initialize(Application app) {
        createTruck();
//        // init values :
//        setParameter(pathIndex);
//        GameNode.attachChild(animalNode); 
//        animal.setLocalTranslation(X_Position, Y_Position, Z_Position);
    }

    @Override
    public void update(float tpf) { 
        if (myApp instanceof ServerMain){
            
            if (drumOnTheTruck){
                randomTimer += tpf;
            }               
            if (randomTimer >= randomTimerDelay && drumOnTheTruck){
                randomTimer = 0;
                ServerMain serverApp = (ServerMain) myApp;
                Server myServer = serverApp.getMyServer();
                RandomEventMessage triggerMess = new RandomEventMessage();
                myServer.broadcast(triggerMess);
                // TODO : broadcast message to clients !!!
                for (int i = 0; i < rigidOilDrum.length; i++) {
                    rigidOilDrum[i].setLinearVelocity(new Vector3f(0, 0, -5));
                }
                drumOnTheTruck = false;
            }
        }
    }
    
    @Override
    protected void cleanup(Application app) {
        
    }

    @Override
    protected void onEnable() {
        for (int i = 0; i < rigidOilDrum.length; i++){
            rigidOilDrum[i].setPhysicsLocation(new Vector3f(X_Position + 1 - i, Y_Position + 3, Z_Position - 2*i));
            rigidOilDrum[i].setPhysicsRotation(new Matrix3f(0, 0, 1,
                                                            0, 1, 0, 
                                                            -1, 0, 0));
        }
    }        

    @Override
    protected void onDisable() {
        // TODO : reset some values (maybe ???)
    }    

    private void createTruck() {
        
        truck = myApp.getAssetManager().loadModel("Models/Old_Truck/Old_Truck.j3o");
        truckNode = new Node();
        truckNode.attachChild(truck);
        truckNode.setLocalTranslation(X_Position, Y_Position, Z_Position);
        CollisionShape truckHull = CollisionShapeFactory.createDynamicMeshShape(findGeom(truck, "Old_Truck-geom-0"));
        RigidBodyControl rigidTruck1 = new RigidBodyControl(truckHull, 2500);
        truck.addControl(rigidTruck1);
        myBulletAppState.getPhysicsSpace().add(rigidTruck1);
        GameNode.attachChild(truckNode);
        
        for (int i = 0; i < rigidOilDrum.length; i++){
            Cylinder drum = new Cylinder(30, 30, 0.7f, 2, true);
            Geometry drumGeom = new Geometry("drum", drum);
            Material drumMat = new Material(myApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            drumMat.setColor("Color", ColorRGBA.Gray);
            drumMat.setTexture("ColorMap", myApp.getAssetManager().loadTexture("Models/drum2/textures/drum1_base_color.png"));
            drumGeom.setMaterial(drumMat);
            Node oilDrumNode = new Node("oilDrum");
            oilDrumNode.attachChild(drumGeom);
            GameNode.attachChild(oilDrumNode);
            drumGeom.rotate(0, 90, 0);
            rigidOilDrum[i] = new RigidBodyControl(100);
            drumGeom.addControl(rigidOilDrum[i]);
            myBulletAppState.getPhysicsSpace().add(rigidOilDrum[i]);            
        }
    }
   
    public void triggerEvent(){
        for (int i = 0; i < rigidOilDrum.length; i++) {
            rigidOilDrum[i].setLinearVelocity(new Vector3f(0, 0, -5));
        }
    }
    
    private Geometry findGeom(Spatial spatial, String name) {
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (int i = 0; i < node.getQuantity(); i++) {
                Spatial child = node.getChild(i);
                Geometry result = findGeom(child, name);
                if (result != null) {
                    return result;
                }
            }
        } else if (spatial instanceof Geometry) {
            if (spatial.getName().startsWith(name)) {
                return (Geometry) spatial;
            }
        }
        return null;
    }
    
    public float[] getX(){
        float X[] = new float[3];
        for (int i = 0; i < 3; i++){
            X[i] = rigidOilDrum[i].getPhysicsLocation().x;
        }
        return X;
    }
    
    public float[] getY(){
        float Y[] = new float[3];
        for (int i = 0; i < 3; i++){
            Y[i] = rigidOilDrum[i].getPhysicsLocation().y;
        }
        return Y;
    }
    
    public float[] getZ(){
        float Z[] = new float[3];
        for (int i = 0; i < 3; i++){
            Z[i] = rigidOilDrum[i].getPhysicsLocation().z;
        }
        return Z;
    }
    
    public void moveTo(float X, float Y, float Z, int index){
        rigidOilDrum[index].setPhysicsLocation(new Vector3f(X, Y, Z));
    }
    
    public void sendInfo(){
        if(myApp instanceof ServerMain){
            ServerMain myServerApp = (ServerMain) myApp;
            Server myServer = myServerApp.getMyServer();
            float[] X = new float[3];
            float[] Y = new float[3];
            float[] Z = new float[3];
            float[][] rot = new float[3][9];
            for (int i = 0; i < rigidOilDrum.length; i++) {
                X[i] = rigidOilDrum[i].getPhysicsLocation().x;
                Y[i] = rigidOilDrum[i].getPhysicsLocation().y;
                Z[i] = rigidOilDrum[i].getPhysicsLocation().z;
                int j = 0;
                for (int row = 0; row < 3; row++){
                    for (int colum = 0; colum < 3; colum++){
                        rot[i][j] = rigidOilDrum[i].getPhysicsRotationMatrix().get(row,colum);
                        j++;
                    }
                    
                }
            }
            DrumPositionMessage posMess = new DrumPositionMessage(X, Y, Z, rot);
            myServer.broadcast(posMess);
        }/*else{
        // just for test
        System.out.println("mygame.Truck.sendInfo()");
        for (int i = 0; i < rigidOilDrum.length; i++) {
        System.out.println("X : "+ rigidOilDrum[i].getPhysicsLocation().x +
        "Y : "+ rigidOilDrum[i].getPhysicsLocation().y +
        "Z : "+ rigidOilDrum[i].getPhysicsLocation().z);
        System.out.println("rot :" +rigidOilDrum[i].getPhysicsRotationMatrix());
        }
        }*/
    }

    void setRotation(float[][] rot, int i) {
        rigidOilDrum[i].setPhysicsRotation(new Matrix3f(rot[i][0], rot[i][1], rot[i][2],
                                                        rot[i][3], rot[i][4], rot[i][5], 
                                                        rot[i][6], rot[i][7], rot[i][8]));
    }
}
