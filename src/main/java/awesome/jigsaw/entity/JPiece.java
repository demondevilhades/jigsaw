package awesome.jigsaw.entity;

import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * 
 * @author awesome
 */
@Builder
@Data
public class JPiece {
    
    private int width;
    private int height;
    private int imageType;
    private List<int[]> whiteList;
    private Map<Integer, List<int[]>> blackListMap;
    
    private  List<GeneralPath> gpList;
}
