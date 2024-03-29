package run.halo.app.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import run.halo.app.model.dto.PhotoDTO;
import run.halo.app.model.entity.Photo;
import run.halo.app.model.params.PhotoParam;
import run.halo.app.model.params.PhotoQuery;
import run.halo.app.model.vo.PhotoTeamVO;
import run.halo.app.service.base.CrudService;

/**
 * Photo service interface.
 *
 * @author johnniang
 * @author ryanwang
 * @date 2019-03-14
 */
public interface PhotoService extends CrudService<Photo, Integer> {

    /**
     * List photo dtos.
     *
     * @param sort sort
     * @return all photos
     */
    List<PhotoDTO> listDtos(@NonNull Sort sort);

    /**
     * Lists photo team vos.
     *
     * @param sort must not be null
     * @return a list of photo team vo
     */
    List<PhotoTeamVO> listTeamVos(@NonNull Sort sort);

    /**
     * List photos by team.
     *
     * @param team team
     * @param sort sort
     * @return list of photos
     */
    List<PhotoDTO> listByTeam(@NonNull String team, Sort sort);

    /**
     * Pages photo output dtos.
     *
     * @param pageable page info must not be null
     * @return a page of photo output dto
     */
    Page<PhotoDTO> pageBy(@NonNull Pageable pageable);

    /**
     * Pages photo output dtos.
     *
     * @param pageable page info must not be null
     * @param photoQuery photoQuery
     * @return a page of photo output dto
     */
    @NonNull
    Page<PhotoDTO> pageDtosBy(@NonNull Pageable pageable, PhotoQuery photoQuery);

    /**
     * Creates photo by photo param.
     *
     * @param photoParam must not be null
     * @return create photo
     */
    @NonNull
    Photo createBy(@NonNull PhotoParam photoParam);

    /**
     * List all teams.
     *
     * @return list of teams
     */
    List<String> listAllTeams();

    /**
     * 根据条件查询
     * @param photoQuery 查询对象
     * @return List<Photo>
     */
    List<Photo> listByCondition(PhotoQuery photoQuery);

    /**
     * 删除图片
     * @param photoList 图片列表
     */
    void deleteByList(List<Photo> photoList);
}
