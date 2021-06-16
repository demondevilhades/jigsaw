package awesome.jigsaw.util;

import java.awt.image.BufferedImage;

import awesome.jigsaw.entity.JPiece;

/**
 * 
 * @author awesome
 */
public interface JigsawUtil {

    /**
     * 
     * @param ptkBufferedImage
     * @return
     */
    public JPiece init(final BufferedImage ptkBufferedImage);

    /**
     * 
     * @param picBufferedImage
     * @param startX
     * @param startY
     * @param jPiece
     * @return
     */
    public BufferedImage[] cut(final BufferedImage picBufferedImage, final int startX, final int startY, final JPiece jPiece);
}
