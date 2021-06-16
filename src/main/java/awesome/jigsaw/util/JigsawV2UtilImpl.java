package awesome.jigsaw.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awesome.jigsaw.entity.JPiece;

/**
 * 
 * @author awesome
 */
public class JigsawV2UtilImpl implements JigsawUtil {

    private final BufferedImage blackCover = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    {
        Graphics2D coverG2 = blackCover.createGraphics();
        coverG2.setColor(Color.BLACK);
        coverG2.fillRect(0, 0, 1, 1);
        coverG2.dispose();
    }

    private final BufferedImage whiteCover = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    {
        Graphics2D coverG2 = whiteCover.createGraphics();
        coverG2.setColor(Color.WHITE);
        coverG2.fillRect(0, 0, 1, 1);
        coverG2.dispose();
    }

    private static final int BORDER_RANGE = 3;

    @Override
    public JPiece init(final BufferedImage ptkBufferedImage) {
        List<int[]> whiteList = new ArrayList<>();
        Map<Integer, List<int[]>> blackListMap = new HashMap<>();
        
        for (int i = 2; i <= BORDER_RANGE; i++) {
            blackListMap.put(i, new ArrayList<>());
        }
        
        for (int x = 0, y; x < ptkBufferedImage.getWidth(); x++) {
            for (y = 0; y < ptkBufferedImage.getHeight(); y++) {
                int rgb = ptkBufferedImage.getRGB(x, y);
                int[] alphaRGB = getAlphaRGB(rgb);
                
                if (alphaRGB[0] != 0) {// 不透明的像素点替换
                    
                    // pic
                    int borderDistance = -1;
                    if(BORDER_RANGE > 0) {
                        borderDistance = getBorderDistance(ptkBufferedImage, x, y, BORDER_RANGE);
                    }
                    if(borderDistance == -1) {
                        blackListMap.get(BORDER_RANGE).add(new int[] {x, y});
                    } else if(borderDistance == 1) {
                        whiteList.add(new int[] {x, y});
                    } else {
                        blackListMap.get(borderDistance).add(new int[] {x, y});
                    }
                }
            }
        }
        return JPiece.builder().width(ptkBufferedImage.getWidth()).height(ptkBufferedImage.getHeight())
                .imageType(ptkBufferedImage.getType()).whiteList(whiteList).blackListMap(blackListMap).build();
    }
    
    @Override
    public BufferedImage[] cut(final BufferedImage picBufferedImage,
            final int startX, final int startY, JPiece jPiece) {
        List<int[]> whiteList = jPiece.getWhiteList();
        Map<Integer, List<int[]>> blackListMap = jPiece.getBlackListMap();
        
        BufferedImage newPicBufferedImage = deepCopy(picBufferedImage);
        Graphics2D graphics2d = newPicBufferedImage.createGraphics();
        applyQualityRenderingHints(graphics2d);
        
        BufferedImage newPtkBufferedImage = new BufferedImage(jPiece.getWidth(), picBufferedImage.getHeight(), jPiece.getImageType());

        graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1f));
        for (int[] white : whiteList) {
            newPtkBufferedImage.setRGB(white[0], startY + white[1], 0xffffffff);
            
            graphics2d.drawImage(whiteCover, startX + white[0], startY + white[1], 1, 1, null);
        }
        for (Map.Entry<Integer, List<int[]>> entry : blackListMap.entrySet()) {
            graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, (1.2f - entry.getKey() * 0.2f)));
            List<int[]> blackList = entry.getValue();
            for (int[] black : blackList) {
                newPtkBufferedImage.setRGB(black[0], startY + black[1], picBufferedImage.getRGB(startX + black[0], startY + black[1]));
                
                graphics2d.drawImage(blackCover, startX + black[0], startY + black[1], 1, 1, null);
            }
        }
        graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        graphics2d.dispose();

        return new BufferedImage[] { newPicBufferedImage, newPtkBufferedImage };
    }

    private void applyQualityRenderingHints(Graphics2D graphics2d) {
        graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        graphics2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        graphics2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

//        graphics2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
//        graphics2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
//        graphics2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
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
}
