package run.halo.app.model.params;

import lombok.Data;
import java.util.List;

/**
 * Photo query params.
 *
 * @author ryanwang
 * @date 2019/04/25
 */
@Data
public class PhotoQuery {

    /**
     * Keyword.
     */
    private String keyword;

    private String team;

    private List<String> urlList;
}
