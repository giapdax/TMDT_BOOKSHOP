package vn.fs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.fs.entities.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

	// favorite
	@Query(value = "SELECT * FROM favorites where product_id  = ? and user_id = ?;", nativeQuery = true)
	public Favorite selectSaves(Long productId, Long userId);

	@Query(value = "SELECT * FROM favorites where user_id = ?;", nativeQuery = true)
	public List<Favorite> selectAllSaves(Long userId);

	@Query(value = "SELECT Count(favorite_id)  FROM favorites  where user_id = ?;", nativeQuery = true)
	public Integer selectCountSave(Long userId);
	@Query("SELECT f.product.productId FROM Favorite f WHERE f.user.userId = :uid AND f.product.productId IN :pids")
	List<Long> findProductIdsByUserIdAndProductIds(@Param("uid") Long uid, @Param("pids") List<Long> productIds);

	@Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Favorite f WHERE f.user.userId = :userId AND f.product.productId = :productId")
	boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

}
