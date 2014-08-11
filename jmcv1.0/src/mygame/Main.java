package mygame;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
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
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.CV_AA;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import org.bytedeco.javacpp.opencv_core.IplImage;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvRectangle;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvCvtColor;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.FrameGrabber;
/**
 * test
 * @author normenhansen
 */
public class Main extends SimpleApplication implements AnimEventListener {
    IplImage iplImage; // we can get a BufferedImage here
    IplImage grayImage;
    CvRect cvRect; // we can get x,y,w,h (rectangle definition) where eyes were detected
    Dimension detectionPoint;
    Dimension detectionArea;
    Geometry backgroundGeom;
    CvMemStorage storage;
    FrameGrabber grabber;
    final String CASCADE_EYEPAIR_BIG = getClass().getClassLoader().
            getResource("resources/cascades/haarcascade_mcs_eyepair_big.xml").getPath();
    final String CASCADE_SMILE = getClass().getClassLoader().
            getResource("resources/cascades/haarcascade_smile.xml").getPath();
    final String CASCADE_FRONTALFACE_ALT2 = getClass().getClassLoader().
            getResource("resources/cascades/haarcascade_frontalface_alt2.xml").getPath();
    CvHaarClassifierCascade classifier;
    Material backgroundMat;
    private AnimChannel channel;
    private AnimControl control;
    Node player;
    AudioNode music;

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
        flyCam.setEnabled(false);
        try {
            classifier = new CvHaarClassifierCascade(cvLoad(CASCADE_FRONTALFACE_ALT2));
            if (classifier.isNull()) {
                System.err.println("Error loading classifier file \"" + CASCADE_FRONTALFACE_ALT2 + "\".");
                System.exit(1);
            }
            storage = CvMemStorage.create();
            try {
                // try camera 0
                grabber = FrameGrabber.createDefault(0);
                grabber.start();
                iplImage = grabber.grab();
            } catch (FrameGrabber.Exception exception) {
                // if camera 0 fails, try camera 1
                grabber = FrameGrabber.createDefault(1);
                grabber.start();
                iplImage = grabber.grab();
            }
            grayImage    = IplImage.create(iplImage.width(), iplImage.height(), IPL_DEPTH_8U, 1);
            detectionArea = new Dimension();
            detectionPoint = new Dimension();
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
            channel.setAnim("Idle");
            music = new AudioNode(assetManager, "Sounds/RichardWagnerRideOfTheValkyries.ogg", true);

        } catch (FrameGrabber.Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        try {
            backgroundMat.setTexture("ColorMap", awtImageToTexture(WebcamDetection()));
        } catch (FrameGrabber.Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (cvRect != null) {
            if (detectionPoint.width < detectionArea.width/2) {
                if (!channel.getAnimationName().equals("Dance")) {
                    channel.setAnim("Dance", 1.0f);
                    channel.setLoopMode(LoopMode.Loop);
                    channel.setSpeed(1f);
                    music.play();
                }
            } else {
                if (!channel.getAnimationName().equals("Idle")) {
                    channel.setAnim("Idle", 0.5f);
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

    public BufferedImage WebcamDetection() throws FrameGrabber.Exception {
        if ((iplImage = grabber.grab()) != null) {
            cvClearMemStorage(storage);
            // In order to detect something a grayscale image is needed.
            cvCvtColor(iplImage, grayImage, CV_BGR2GRAY);
            CvSeq detectedObject = cvHaarDetectObjects(grayImage, classifier, storage,
                    1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
            int total = detectedObject.total();
            for (int i = 0; i < total; i++) {
                cvRect = new CvRect(cvGetSeqElem(detectedObject, i));
                int x = cvRect.x(), y = cvRect.y(), w = cvRect.width(), h = cvRect.height();
                // reduce the rectangle
                x+=w/4;
                y+=h/4;
                w=w/2;
                h=h/2;
                cvRectangle(iplImage, cvPoint(x, y), cvPoint(x + w, y + h), CvScalar.MAGENTA, 1, CV_AA, 0);
                detectionArea.setSize(iplImage.width(), iplImage.height());
                detectionPoint.setSize(x + w/2, y + h/2);
            }
        }
        return iplImage.getBufferedImage();
    }

    public static Texture awtImageToTexture(final BufferedImage img) {
        final AWTLoader loader = new AWTLoader();
        final Texture tex = new Texture2D();
        tex.setImage(loader.load(img, true));
        return tex;
    }

    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equals("Dance")) {
            channel.setAnim("Idle", 0.50f);
            channel.setLoopMode(LoopMode.DontLoop);
            channel.setSpeed(1f);
        }
    }

    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
        // unused
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
}
