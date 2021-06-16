package awesome.jigsaw.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import awesome.jigsaw.entity.JPiece;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author awesome
 */
@Slf4j
public class JigsawV3UtilImpl implements JigsawUtil {

    private static final int BORDER_RANGE = 4;

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
        
        List<GeneralPath> gpList = new ArrayList<>();
        gpList.add(pointList2GP(whiteList));
        gpList.add(pointList2GP(blackListMap.get(2)));
        gpList.add(pointList2GP(blackListMap.get(3)));
        
        return JPiece.builder().width(ptkBufferedImage.getWidth()).height(ptkBufferedImage.getHeight())
                .imageType(ptkBufferedImage.getType()).whiteList(whiteList).blackListMap(blackListMap).gpList(gpList).build();
    }
    
    @Override
    public BufferedImage[] cut(final BufferedImage picBufferedImage,
            final int startX, final int startY, JPiece jPiece) {
        BufferedImage newPtkBufferedImage = new BufferedImage(jPiece.getWidth(), picBufferedImage.getHeight(), jPiece.getImageType());
        {
            List<int[]> whiteList = jPiece.getWhiteList();
            Map<Integer, List<int[]>> blackListMap = jPiece.getBlackListMap();
            for (int[] white : whiteList) {
                newPtkBufferedImage.setRGB(white[0], startY + white[1], 0xffffffff);
            }
            for (Map.Entry<Integer, List<int[]>> entry : blackListMap.entrySet()) {
                List<int[]> blackList = entry.getValue();
                for (int[] black : blackList) {
                    newPtkBufferedImage.setRGB(black[0], startY + black[1], picBufferedImage.getRGB(startX + black[0], startY + black[1]));
                }
            }
        }

        BufferedImage newPicBufferedImage = deepCopy(picBufferedImage);
        {
            List<GeneralPath> gpList = jPiece.getGpList();

            Graphics2D graphics2d = newPicBufferedImage.createGraphics();
            applyQualityRenderingHints(graphics2d);
            
            graphics2d.translate(startX, startY);

            graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.8f));
            graphics2d.setColor(Color.WHITE);
            graphics2d.draw(gpList.get(0));

            graphics2d.setColor(Color.BLACK);
            graphics2d.draw(gpList.get(1));

            graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.6f));
            graphics2d.draw(gpList.get(2));

            graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.4f));
            graphics2d.fill(gpList.get(2));
            
            graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
            graphics2d.dispose();
        }

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

    private GeneralPath pointList2GP(final List<int[]> pointList) {
        List<int[]> list = new ArrayList<>(pointList);
        GeneralPath gp = new GeneralPath();

        int[] startPoint = list.get(0);
        int[] point = startPoint;
        gp.moveTo(point[0], point[1]);
        list.remove(0);

        int[] nextPoint = null;
        
        while (list.size() > 0) {
            double distance = Double.MAX_VALUE;
            int index = -1;
            for (int i = 0; i < list.size(); i++) {
                int[] is = list.get(i);
                double distance2 = distance(point[0], point[1], is[0], is[1]);
                if(distance > distance2) {
                    distance = distance2;
                    nextPoint = is;
                    index = i;
                }
            }
            if (distance >= 2) {
                log.warn("point.x = {}, point.y = {}, nextPoint.x = {}, nextPoint.y = {}, distance = {}", point[0],
                        point[1], nextPoint[0], nextPoint[1], distance);
            }
            if(distance > 3) {
                break;
            }
            gp.lineTo(nextPoint[0], nextPoint[1]);
            point = nextPoint;
            list.remove(index);
        }
        double distance = distance(point[0], point[1], startPoint[0], startPoint[1]);
        if(distance(point[0], point[1], startPoint[0], startPoint[1]) < 3) {
            log.info("pointList2GP success");
            gp.closePath();
        } else {
            log.error("pointList2GP failed : distance = {}", distance);
            throw new RuntimeException("pointList2GP failed");
        }
        return gp;
    }
    
    private double distance(int x0, int y0, int x1, int y1) {
        return Math.sqrt(Math.pow(x0 - x1, 2) + Math.pow(y0 - y1, 2));
    }
}
