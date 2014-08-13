package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.duo.jmcv.Jmcv;
import org.duo.jmcv.JmcvWebcamDetectionException;


/**
 * test
 * @author normenhansen
 */
public class Main extends SimpleApplication implements AnimEventListener {
    Geometry backgroundGeom;
    Material backgroundMat;
    private AnimChannel channel;
    private AnimControl control;
    Node player;
    AudioNode music;
    Jmcv jmcv;
    Random random;

    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Jogo Básico");
        settings.setSettingsDialogImage("Interface/splashscreen.png");
        settings.setResolution(1280, 800);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        jmcv = new Jmcv();
        flyCam.setEnabled(false);
        backgroundMat = new Material(assetManager, "MatDefs/Unshaded.j3md");
        Texture backgroundTex = assetManager.loadTexture("Interface/Logo/Monkey.png");
        backgroundMat.setTexture("ColorMap", backgroundTex);
        float w = this.getContext().getSettings().getWidth();
        float h = this.getContext().getSettings().getHeight();
        float ratio = w / h;
        cam.setLocation(Vector3f.ZERO.add(new Vector3f(0.0f, 0.0f, 1.1f)));//Move the Camera back just a bit.

        float width = 1 * ratio;
        float height = 1;

        Quad fsq = new Quad(width, height);
        backgroundGeom = new Geometry("Background", fsq);
        backgroundGeom.setQueueBucket(Bucket.Sky);
        backgroundGeom.setCullHint(CullHint.Never);
        backgroundGeom.setMaterial(backgroundMat);
        backgroundGeom.setLocalTranslation(-(width / 2), -(height / 2), 0);  //Need to Divide by two because the quad origin is bottom left
        rootNode.attachChild(backgroundGeom);
        initKeys();
        rootNode.addLight(new AmbientLight());
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -1f, -1).normalizeLocal());
        rootNode.addLight(dl);
        player = (Node) assetManager.loadModel("Models/gilb.j3o");
        player.setLocalScale(0.2f);
        player.setLocalTranslation(new Vector3f(0.0f, -0.3f, -0.2f));
        rootNode.attachChild(player);
        control = player.getControl(AnimControl.class);
        control.addListener(this);
        channel = control.createChannel();
        channel.setAnim("Idle", 0.5f);
        channel.setLoopMode(LoopMode.Loop);
        channel.setSpeed(0.4f);
        music = new AudioNode(assetManager, "Sounds/RichardWagnerRideOfTheValkyries.ogg", true);
    }

    @Override
    public void simpleUpdate(float tpf) {
        try {
            backgroundMat.setTexture("ColorMap", awtImageToTexture(jmcv.WebcamDetection()));
        } catch (JmcvWebcamDetectionException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (jmcv.isDetectionOk()) {
            if (jmcv.getDetectionPoint().width < jmcv.getDetectionArea().width/3) {
                if (!channel.getAnimationName().equals("Dance")) {
                    channel.setAnim("Dance", 1.0f);
                    channel.setLoopMode(LoopMode.Loop);
                    channel.setSpeed(1f);
                    music.play();
                }
            } else if (jmcv.getDetectionPoint().width < 2* jmcv.getDetectionArea().width/3) {
                if (!channel.getAnimationName().equals("Idle")) {
                    channel.setAnim("Idle", 0.5f);
                    channel.setLoopMode(LoopMode.Loop);
                    channel.setSpeed(0.4f);
                    music.pause();
                }
            } else {
                if (!channel.getAnimationName().equals("Negate")) {
                    channel.setAnim("Negate", 1.0f);
                    channel.setLoopMode(LoopMode.Loop);
                    channel.setSpeed(1f);
                    music.pause();
                }
            }
        }
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    public static Texture awtImageToTexture(final BufferedImage img) {
        final AWTLoader loader = new AWTLoader();
        final Texture tex = new Texture2D();
        tex.setImage(loader.load(img, true));
        return tex;
    }

    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        rotatePlayerRandomly();
        if (animName.equals("Dance")) {
            channel.setAnim("Idle", 0.50f);
            channel.setLoopMode(LoopMode.DontLoop);
            channel.setSpeed(1f);
        }
    }

    @Override
    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        rotatePlayerRandomly();
    }

    private void initKeys() {
        inputManager.addMapping("Dance", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(actionListener, "Dance");
    }

    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("Dance") && !keyPressed) {
                if (!channel.getAnimationName().equals("Dance")) {
                    channel.setAnim("Dance", 0.50f);
                    channel.setLoopMode(LoopMode.Loop);
                }
            }
        }
    };

    private void rotatePlayerRandomly() {
        // rotate char a bit
        if(random==null) {
            random = new Random();
        }
        float[] angles = new float[3];
        player.getLocalRotation().toAngles(angles);
        int angleIndex = random.nextInt(3);
        float rotateAngle;
        // choose between -45, 0, 45 (facing 45 degrees to the left, 
        // front, 45 degrees to the right
        switch(angleIndex) {
            case 0:
                rotateAngle = -45.0f;
                break;
            case 1:
                rotateAngle = 0.0f;
                break;
            default:
                rotateAngle = 45.0f;
        }
        angles[1] = rotateAngle;
        player.setLocalRotation(new Quaternion(angles));
    }
}
