package awesome.jigsaw.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import awesome.jigsaw.entity.JPiece;

/**
 * 
 * @author awesome
 */
public class JigsawUtilImpl implements JigsawUtil {

    private static final float FACTOR = 0.7f;
    private static final int BORDER_RANGE = 3;

    /**
     * 
     * @param picBufferedImage
     * @param ptkBufferedImage
     * @param startX
     * @param startY
     * @param jPiece
     * @return
     */
    public BufferedImage[] cut(final BufferedImage picBufferedImage, final BufferedImage ptkBufferedImage,
            final int startX, final int startY, final JPiece jPiece) {
        BufferedImage newPicBufferedImage = deepCopy(picBufferedImage);
        BufferedImage newPtkBufferedImage = new BufferedImage(ptkBufferedImage.getWidth(), picBufferedImage.getHeight(), ptkBufferedImage.getType());
        
        for (int x = 0, y; x < ptkBufferedImage.getWidth(); x++) {
            for (y = 0; y < ptkBufferedImage.getHeight(); y++) {
                int rgb = ptkBufferedImage.getRGB(x, y);
                int[] alphaRGB = getAlphaRGB(rgb);
                if (alphaRGB[0] != 0) {// 不透明的像素点替换
                    int originRGB = picBufferedImage.getRGB(startX + x, startY + y);
                    
                    int borderDistance = -1;
                    if(BORDER_RANGE > 0) {
                        borderDistance = getBorderDistance(ptkBufferedImage, x, y, BORDER_RANGE);
                    }
                    if(borderDistance == 1) {
                        newPicBufferedImage.setRGB(startX + x, startY + y, Color.WHITE.getRGB());
                        newPtkBufferedImage.setRGB(x, startY + y, Color.WHITE.getRGB());
                    } else if(borderDistance > 1) {
                        int darkRGB = darker(originRGB, (BORDER_RANGE - borderDistance) * 2 + 2);
                        newPicBufferedImage.setRGB(startX + x, startY + y, darkRGB);
                        newPtkBufferedImage.setRGB(x, startY + y, originRGB);
                    } else {
                        newPicBufferedImage.setRGB(startX + x, startY + y, darker(originRGB, 2));
                        newPtkBufferedImage.setRGB(x, startY + y, originRGB);
                    }
                }
            }
        }
        return new BufferedImage[] { newPicBufferedImage, newPtkBufferedImage };
    }

    /**
     * 内到外检查
     * 
     * @param bufferedImage
     *            原始图片
     * @param x
     *            当前坐标x
     * @param y
     *            当前坐标y
     * @param range
     *            区域半径
     * @return 真实区域半径
     */
    @SuppressWarnings("unused")
    private int getBlurRangeI2O(final BufferedImage bufferedImage, final int x, final int y, final int range) {
        for (int i = 1; i <= range; i++) {
            if (x - i < 0 || y - i < 0 || x + i >= bufferedImage.getWidth() || y + i >= bufferedImage.getHeight()) {
                return range - i;
            }
            if (getAlpha(bufferedImage.getRGB(x - i, y + i)) == 0 || getAlpha(bufferedImage.getRGB(x - i, y - i)) == 0
                    || getAlpha(bufferedImage.getRGB(x + i, y + i)) == 0
                    || getAlpha(bufferedImage.getRGB(x + i, y - i)) == 0) {
                return range - i;
            }
        }
        return 0;
    }
    
    /**
     * 距离边界距离
     * 
     * @param bufferedImage
     * @param x
     * @param y
     * @param range
     * @return
     */
    private int getBorderDistance(final BufferedImage bufferedImage, final int x, final int y, final int range) {
        for (int i = 1; i <= range; i++) {
            if (x - i < 0 || y - i < 0 || x + i >= bufferedImage.getWidth() || y + i >= bufferedImage.getHeight()) {
                return i;
            }
            if (getAlpha(bufferedImage.getRGB(x - i, y + i)) == 0 || getAlpha(bufferedImage.getRGB(x - i, y - i)) == 0
                    || getAlpha(bufferedImage.getRGB(x + i, y + i)) == 0
                    || getAlpha(bufferedImage.getRGB(x + i, y - i)) == 0) {
                return i;
            }
        }
        return -1;
    }

    @SuppressWarnings("unused")
    private int brighter(final int argb) {
        int[] alphaRGB = getAlphaRGB(argb);
        return (alphaRGB[0] << 24) + brighter(alphaRGB[1], alphaRGB[2], alphaRGB[3]);
    }
    
    private int darker(final int argb, final int times) {
        int[] alphaRGB = getAlphaRGB(argb);
        return (alphaRGB[0] << 24) + darker(alphaRGB[1], alphaRGB[2], alphaRGB[3], times);
    }
    
    private int brighter(final int r, final int g, final int b) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        return Color.HSBtoRGB(hsb[0], Math.min(hsb[1] / FACTOR, 1f), hsb[2]);
    }
    
    private int darker(final int r, final int g, final int b, int times) {
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        while (times-- > 0) {
            hsb[2] *= FACTOR;
        }
        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * 
     * @param argb
     * @return [alpha, r, g, b]
     */
    private int[] getAlphaRGB(final int argb) {
        return new int[] { (argb >> 24 & 0xff), (argb >> 16 & 0xff), (argb >> 8 & 0xff), (argb & 0xff) };
    }

    /**
     * 
     * @param rgb
     * @return
     */
    private int getAlpha(final int rgb) {
        return (rgb >> 24 & 0xff);
    }

    private BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        return new BufferedImage(cm, bi.copyData(null), cm.isAlphaPremultiplied(), null);
    }

    @Override
    public JPiece init(BufferedImage ptkBufferedImage) {
        return null;
    }

    @Override
    public BufferedImage[] cut(BufferedImage picBufferedImage, int startX, int startY, JPiece jPiece) {
        throw new UnsupportedOperationException();
    }
}
