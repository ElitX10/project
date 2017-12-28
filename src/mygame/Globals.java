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
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializer;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;

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
//        Serializer.registerClass(TimeMessage.class);
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
    private AudioNode StartSoundNode;
    private AudioNode StopSoundNode;
    private CameraNode camNode;
    private final Node NODE_GAME;
    private final BulletAppState myBulletAppState;
    private final int ID;
    
    private static int numberOfPlayer = 0;
    
    public Player(SimpleApplication app, Node gameNode,BulletAppState bulletAppState){
        myApp = app;
        NODE_GAME = gameNode;
        myBulletAppState = bulletAppState;
        
        // increase the number of player every time we create one player  
        numberOfPlayer++;
        
        // give an id to every player to separate input later and for displaying the score on the sceen for each player :
        this.ID = numberOfPlayer;
    }
    
    public static void resetNumberOfPlayer(){
        numberOfPlayer = 0;
    }
    
    @Override
    protected void initialize(Application app) {
        buildPlayer();
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
        player.setPhysicsRotation(new Matrix3f( 0, 0, 1,
                                                0, 1, 0, 
                                                -1, 0, 0));
    }

    @Override
    protected void onDisable() {
        NODE_GAME.detachChild(carNode);
        getPhysicsSpace().remove(player);
    }
    
    private void buildPlayer() {
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
        
        // camera following the car :
        myApp.getFlyByCamera().setEnabled(false);
//        flyCam.setEnabled(false);
        camNode = new CameraNode("Camera Node", myApp.getCamera());
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        carNode.attachChild(camNode);
        camNode.setLocalTranslation(new Vector3f(0, 5, 15));
        camNode.lookAt(carNode.getLocalTranslation(), Vector3f.UNIT_Y);
        
        // car sound :
        AudioNode engineNoiseNode = new AudioNode(myApp.getAssetManager(), "Sounds/engineNoise.ogg");
        engineNoiseNode.setPositional(true);
        engineNoiseNode.setLooping(true);
        engineNoiseNode.setVolume(0.1f);
        carNode.attachChild(engineNoiseNode);
        engineNoiseNode.play();
        
        accelerationSoundNode = new AudioNode(myApp.getAssetManager(), "Sounds/acceleration.ogg");
        accelerationSoundNode.setPositional(true);
        accelerationSoundNode.setLooping(false);
        accelerationSoundNode.setVolume(0.3f);
        carNode.attachChild(accelerationSoundNode);
        
        StartSoundNode = new AudioNode(myApp.getAssetManager(), "Sounds/Start.ogg");
        StartSoundNode.setPositional(true);
        StartSoundNode.setLooping(false);
        StartSoundNode.setVolume(0.3f);
        carNode.attachChild(StartSoundNode);
        
        StopSoundNode = new AudioNode(myApp.getAssetManager(), "Sounds/stoping.ogg");
        StopSoundNode.setPositional(true);
        StopSoundNode.setLooping(false);
        StopSoundNode.setVolume(0.3f);
        carNode.attachChild(StopSoundNode);        
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
}

//-------------------------------------------------WOLF---------------------------------------------------------------------------------------
class Wolf extends BaseAppState{
    private int GlobleXPosition = 0;
    private int GlobleYPosition = 0;
    private int GlobleZPosition = 0;
    
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