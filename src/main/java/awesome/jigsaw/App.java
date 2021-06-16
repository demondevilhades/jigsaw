package awesome.jigsaw;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.imageio.ImageIO;

import awesome.jigsaw.entity.JPiece;
import awesome.jigsaw.util.JigsawUtil;
import awesome.jigsaw.util.JigsawV2UtilImpl;
import awesome.jigsaw.util.JigsawV3UtilImpl;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author awesome
 */
@Slf4j
public class App {

    private final String mapFilePath = "./map";
    private BufferedImage[] jMapBIs = null;

    private final String pieceFilePath = "./piece";
    private JPiece[] jPieces = null;

    private final ReadWriteLock mapRWLock = new ReentrantReadWriteLock();
    private final Random random = new Random();

    private JigsawUtil jigsawUtil;

    private final int padding = 20;

    /**
     * 
     * @throws IOException
     */
    public void init() throws IOException {
        log.info("init");
        if (mapRWLock.writeLock().tryLock()) {
            try {
                jigsawUtil = new JigsawV3UtilImpl();
                flushJMap();
                flushJPiece();
                log.info("use JigsawV3UtilImpl");
            } catch (IOException e) {
                log.info("use v2 : {}", e.getMessage());
                jigsawUtil = new JigsawV2UtilImpl();
                flushJMap();
                flushJPiece();
                log.info("use JigsawV2UtilImpl");
            } finally {
                mapRWLock.writeLock().unlock();
            }
        }
    }

    private void flushJMap() throws IOException {
        File mapDir = new File(getPath(mapFilePath));
        File[] files = mapDir.listFiles();
        jMapBIs = new BufferedImage[files.length];
        for (int i = 0; i < files.length; i++) {
            jMapBIs[i] = ImageIO.read(files[i]);
        }
    }

    private void flushJPiece() throws IOException {
        File pieceDir = new File(getPath(pieceFilePath));
        File[] files = pieceDir.listFiles();
        jPieces = new JPiece[files.length];
        for (int i = 0; i < files.length; i++) {
            jPieces[i] = jigsawUtil.init(ImageIO.read(files[i]));
        }
    }

    /**
     * 
     * @return
     */
    public String[] generate() {
        log.info("generate");

        BufferedImage jMapBI = null;
        JPiece jPiece = null;
        try {
            mapRWLock.readLock().lock();
            jMapBI = jMapBIs[random.nextInt(jMapBIs.length)];
            jPiece = jPieces[random.nextInt(jPieces.length)];
        } finally {
            mapRWLock.readLock().unlock();
        }
        if (jPiece == null || jMapBI == null) {
            return null;
        }

        try {
            long currentTimeMillis = System.currentTimeMillis();

            int startX = padding + jPiece.getWidth()
                    + random.nextInt(jMapBI.getWidth() - padding * 2 - jPiece.getWidth() * 2);
            int startY = padding + random.nextInt(jMapBI.getHeight() - padding * 2 - jPiece.getHeight());

            BufferedImage[] cutWithBlur = jigsawUtil.cut(jMapBI, startX, startY, jPiece);

            String jMapStr = getImageBase64(cutWithBlur[0], "jpg");
            String jPieceStr = getImageBase64(cutWithBlur[1], "png");

            log.info("generate ： x = {}, y = {}, time = {}", startX, startY,
                    (System.currentTimeMillis() - currentTimeMillis));
            return new String[] { jMapStr, jPieceStr };
        } catch (IOException e) {
            log.error("", e);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    private String getImageBase64(final BufferedImage bufferedImage, final String formatName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, formatName, baos);
        byte[] bs = baos.toByteArray();
        return Base64.getEncoder().encodeToString(bs);
    }
    
    private String getPath(String str) {
        return this.getClass().getClassLoader().getResource(str).getFile();
    }

    /**
     * 
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        App app = new App();
        app.init();
        String[] strs = app.generate();
        log.info("jMap = {}", strs[0]);
        log.info("jPiece = {}", strs[1]);
        // test : https://tool.jisuapi.com/base642pic.html
    }
}
