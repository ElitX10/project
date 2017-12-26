package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import static com.jme3.bullet.PhysicsSpace.getPhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.MotionPathListener;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.shape.Box;
import java.util.Random;

/**
 * This is the Main Class of your Game. You should only do initialization here.
 * Move your Logic into AppStates or Controls
 * @author normenhansen
 */
public class Main extends SimpleApplication{

    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }
    private BulletAppState bulletAppState;
    private Node carNode;
    private VehicleControl player;
    private float wheelRadius;
    private float steeringValue = 0;
    private float accelerationValue = 0;
    private CameraNode camNode;
    private MotionPath path;
    private MotionEvent motionControl;
    private AudioNode accelerationSoundNode;
    private AudioNode StartSoundNode;
    private AudioNode StopSoundNode;
    private final Random myRand = new Random();
    private int randomTimerDelay = myRand.nextInt((60 - 20) + 1) + 20;
    private float randomTimer = 0;
    
    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        buildPlayer();
        buildFloor();
        setupKeys();
        movingAnimal();
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.5f, -1f, -0.3f).normalizeLocal());
        rootNode.addLight(dl);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(0.5f, -0.1f, 0.3f).normalizeLocal());
//        motionControl.play();
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
        randomTimingForAnimal(tpf);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
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
        return bulletAppState.getPhysicsSpace();
    }
    
    private void buildPlayer() {
        float stiffness = 120.0f;//200=f1 car
        float compValue = 0.2f; //(lower than damp!)
        float dampValue = 0.3f;
        final float mass = 400;
        
        //Load model and get chassis Geometry
        carNode = (Node)assetManager.loadModel("Models/Ferrari/Car.scene");
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

        rootNode.attachChild(carNode);
        getPhysicsSpace().add(player);
        
        // camera following the car :
        flyCam.setEnabled(false);
        camNode = new CameraNode("Camera Node", cam);
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        carNode.attachChild(camNode);
        camNode.setLocalTranslation(new Vector3f(0, 5, 15));
        camNode.lookAt(carNode.getLocalTranslation(), Vector3f.UNIT_Y);
        
        // car sound :
        AudioNode engineNoiseNode = new AudioNode(assetManager, "Sounds/engineNoise.ogg");
        engineNoiseNode.setPositional(true);
        engineNoiseNode.setLooping(true);
        engineNoiseNode.setVolume(0.1f);
        carNode.attachChild(engineNoiseNode);
        engineNoiseNode.play();
        
        accelerationSoundNode = new AudioNode(assetManager, "Sounds/acceleration.ogg");
        accelerationSoundNode.setPositional(true);
        accelerationSoundNode.setLooping(false);
        accelerationSoundNode.setVolume(0.3f);
        carNode.attachChild(accelerationSoundNode);
        
        StartSoundNode = new AudioNode(assetManager, "Sounds/Start.ogg");
        StartSoundNode.setPositional(true);
        StartSoundNode.setLooping(false);
        StartSoundNode.setVolume(0.3f);
        carNode.attachChild(StartSoundNode);
        
        StopSoundNode = new AudioNode(assetManager, "Sounds/stoping.ogg");
        StopSoundNode.setPositional(true);
        StopSoundNode.setLooping(false);
        StopSoundNode.setVolume(0.3f);
        carNode.attachChild(StopSoundNode);
        
        player.setPhysicsLocation(new Vector3f(100, 2, 0));
//        System.out.println("mygame.Main.buildPlayer()" + player.getPhysicsRotationMatrix());
        player.setPhysicsRotation(new Matrix3f( 0, 0, 1,
                                                0, 1, 0, 
                                                -1, 0, 0));
    }

    private void buildFloor() {
//        Box floor = new Box(100, 0.1f, 100);
//        Geometry floorGeom = new Geometry("floor", floor);
//        Material floorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        floorMat.setColor("Color", ColorRGBA.Blue);
//        floorGeom.setMaterial(floorMat);
//        Node floorNode = new Node("floorNode");
//        floorNode.setLocalTranslation(0, -2, 2);
//        floorNode.attachChild(floorGeom);
//        rootNode.attachChild(floorNode);
//        
//        CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(floorNode);
//        RigidBodyControl landscape = new RigidBodyControl(sceneShape, 0);
//        floorNode.addControl(landscape);
//        getPhysicsSpace().add(landscape);
//        
        Spatial gameLevel = assetManager.loadModel("Scenes/Road.j3o");
        gameLevel.scale(3);
        Node road = new Node("landscape");
        road.setLocalTranslation(-50,0,-80);
        road.attachChild(gameLevel);
        rootNode.attachChild(road);
        
        CollisionShape sceneShape2 = CollisionShapeFactory.createMeshShape(road);
        RigidBodyControl landscape2 = new RigidBodyControl(sceneShape2, 0);
        road.addControl(landscape2);
        getPhysicsSpace().add(landscape2);
    }
    
    private void setupKeys() {
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
//        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(actionListener, "Lefts");
        inputManager.addListener(actionListener, "Rights");
//        inputManager.addListener(analogListener, "Ups");
        inputManager.addListener(actionListener, "Ups");
        inputManager.addListener(actionListener, "Downs");
//        inputManager.addListener(this, "Space");
        inputManager.addListener(actionListener, "Reset");
    }
    
//    private final AnalogListener analogListener = new AnalogListener() {
//        @Override
//        public void onAnalog(String name, float value, float tpf) {
//            if(name.equals("Ups")){
//                System.out.println(".onAnalog()");
//            }
//        }
//    };
    
    private final ActionListener actionListener = new ActionListener() {
        private boolean stop = false;
        private boolean goBack = false;
        
        public void onAction(String binding, boolean keyPressed, float tpf) {            
            if (binding.equals("Lefts")) {
                if (keyPressed) {
                    steeringValue += .15f;
                } else {
                    steeringValue += -.15f;
                }
                player.steer(steeringValue);
            } else if (binding.equals("Rights")) {
                if (keyPressed) {
                    steeringValue += -.15f;
                } else {
                    steeringValue += .15f;
                }
                player.steer(steeringValue);
            } //note that our fancy car actually goes backwards..
            else if (binding.equals("Ups")) {
                if (keyPressed) {
                    accelerationValue = - 800;
                    if (player.getCurrentVehicleSpeedKmHour() <= 0.2f && player.getCurrentVehicleSpeedKmHour() >= -0.2f){
                        StartSoundNode.play();
                    }else{
                        accelerationSoundNode.play();  
                    }                    
                    StopSoundNode.stop();
                } else {
                    accelerationValue = 0;
                    accelerationSoundNode.stop();
                    StartSoundNode.stop();                    
                    StopSoundNode.play();
                }
            }
            if (binding.equals("Downs")) {
                if (keyPressed) {
                    stop = true;
                    if (player.getCurrentVehicleSpeedKmHour() <= 0.2f && player.getCurrentVehicleSpeedKmHour() >= -0.2f){
                        goBack = true;
                        accelerationValue = 300;
                        stop = false;
                    }
                } else {
                    accelerationValue = 0;
                    player.accelerate(accelerationValue);
                    stop = false;
                    goBack = false;
                }
            } else if (binding.equals("Reset")) {
                if (keyPressed) {
                    System.out.println("Reset");
                    player.setPhysicsLocation(Vector3f.ZERO);
                    player.setPhysicsRotation(new Matrix3f());
                    player.setLinearVelocity(Vector3f.ZERO);
                    player.setAngularVelocity(Vector3f.ZERO);
                    player.resetSuspension();
                } else {
                }
            }
            
            if (keyPressed){
                if (goBack){                        
                    player.brake(0f);
                    player.accelerate(accelerationValue);
                } else {
                    if (stop){
                        player.brake(30f); 
                        accelerationValue = 0;
                        player.accelerate(accelerationValue);
                    }else{
                        player.brake(0f);
                        player.accelerate(accelerationValue);
                    }
                }
                
                
            }else {
                player.brake(5f);
            }
        }  
    }; 
    
    private void movingAnimal(){
        final int X_GlobalPosition = 50;
        final int Y_GlobalPosition = 0;
        final int Z_GlobalPosition = -5;
        
        // create the animal :
        Spatial animal = assetManager.loadModel("Models/Wolf.j3o");
//        Box animal = new Box(1, 1, 1);
//        Geometry animalGeom = new Geometry("animal", animal);
        Material animalMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        animalMat.setColor("Color", ColorRGBA.Black);
        animal.setMaterial(animalMat);
        Node animalNode = new Node();
        animalNode.attachChild(animal);
        animalNode.setLocalTranslation(0, 2, 0);
        rootNode.attachChild(animalNode);
        animal.rotate(0, 90, 0);
//        CollisionShape animalShape = CollisionShapeFactory.createMeshShape(animalNode);
//        MeshCollisionShape animalShape = new MeshCollisionShape(animal.getMesh());
        RigidBodyControl wolf = new RigidBodyControl(500);
        animalNode.addControl(wolf);
        getPhysicsSpace().add(wolf);
        
//        // create the path :
//        path = new MotionPath();
//        path.addWayPoint(new Vector3f(X_GlobalPosition + 0, Y_GlobalPosition + 0, Z_GlobalPosition - 45));
//        path.addWayPoint(new Vector3f(X_GlobalPosition + 0, Y_GlobalPosition + 3, Z_GlobalPosition - 35));
//        path.addWayPoint(new Vector3f(X_GlobalPosition + 0, Y_GlobalPosition + 3, Z_GlobalPosition - 15));
//        path.addWayPoint(new Vector3f(X_GlobalPosition + 0, Y_GlobalPosition + 0, Z_GlobalPosition - 10));
//        path.addWayPoint(new Vector3f(X_GlobalPosition + 0, Y_GlobalPosition + 0, Z_GlobalPosition + 5));
//        path.addWayPoint(new Vector3f(X_GlobalPosition + 0, Y_GlobalPosition + 3, Z_GlobalPosition + 10));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 0, Y_GlobalPosition + 3, Z_GlobalPosition + 30));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 0, Y_GlobalPosition + 0, Z_GlobalPosition + 40));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 40, Y_GlobalPosition + 0, Z_GlobalPosition + 40));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 40, Y_GlobalPosition + 3, Z_GlobalPosition + 30));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 40, Y_GlobalPosition + 3, Z_GlobalPosition + 10));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 40, Y_GlobalPosition + 0, Z_GlobalPosition + 5));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 40, Y_GlobalPosition + 0, Z_GlobalPosition - 10));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 40, Y_GlobalPosition + 3, Z_GlobalPosition - 15));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 40, Y_GlobalPosition + 3, Z_GlobalPosition - 35));
//        path.addWayPoint(new Vector3f(X_GlobalPosition - 40, Y_GlobalPosition + 0, Z_GlobalPosition - 45));
//        path.addWayPoint(new Vector3f(X_GlobalPosition + 0, Y_GlobalPosition + 0, Z_GlobalPosition - 45));
//        path.enableDebugShape(assetManager, rootNode);
//        path.setCurveTension(0.25f);
//        
//        motionControl = new MotionEvent(animalNode,path);
//        motionControl.setDirectionType(MotionEvent.Direction.PathAndRotation);
//        motionControl.setRotation(new Quaternion().fromAngleNormalAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y));
//        motionControl.setInitialDuration(20f);
//        motionControl.setSpeed(1f);
//        motionControl.play(); to start the animation :        
    }

    private void randomTimingForAnimal(float tpf) {
        randomTimer += tpf;
        if (randomTimer >= randomTimerDelay){
//            motionControl.play();
            randomTimer = 0;
            randomTimerDelay = myRand.nextInt((60 - 20) + 1) + 20;
//            System.out.println("mygame.Main.randomTimingForAnimal() "+ randomTimerDelay);
        }
    }
}
