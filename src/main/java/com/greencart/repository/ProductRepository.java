package com.greencart.repository;

import com.greencart.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select p from Product p where p.deletedAt is null and " +
            "(:q is null or lower(p.name) like lower(concat('%',:q,'%'))) and " +
            "(:cid is null or p.category.id = :cid)")
    Page<Product> search(@Param("q") String q, @Param("cid") Long cid, Pageable p);

    @Query("select p from Product p where p.deletedAt is null and " +
            "(:q is null or lower(p.name) like lower(concat('%',:q,'%'))) and " +
            "(:cid is null or p.category.id = :cid) and " +
            "(:stockMode = 'ALL' or " +
            "(:stockMode = 'LOW' and p.stock > 0 and p.stock <= :threshold) or " +
            "(:stockMode = 'OUT' and p.stock = 0) or " +
            "(:stockMode = 'IN' and p.stock > :threshold))")
    Page<Product> searchAdmin(@Param("q") String q,
                              @Param("cid") Long cid,
                              @Param("stockMode") String stockMode,
                              @Param("threshold") Integer threshold,
                              Pageable p);

    @Query("select p from Product p where p.deletedAt is null order by p.id desc")
    List<Product> findTop24Active(Pageable pageable);

    default List<Product> findTop24ByOrderByIdDesc() {
        return findTop24Active(PageRequest.of(0, 24));
    }

    @Query("select p from Product p where p.deletedAt is null and p.discount > :discount")
    Page<Product> findActiveOffers(@Param("discount") BigDecimal discount, Pageable pageable);

    @Query("select p from Product p where p.deletedAt is null and lower(p.name) = lower(:name)")
    Optional<Product> findActiveByNameIgnoreCase(@Param("name") String name);

    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    List<Product> findByDeletedAtIsNullOrderByIdDesc();
    List<Product> findByCategory_IdAndDeletedAtIsNullOrderByIdDesc(Long categoryId);
    long countByCategory_IdAndDeletedAtIsNull(Long categoryId);
    long countByDeletedAtIsNull();

    List<Product> findByDeletedAtIsNullAndStockLessThanEqualAndStockGreaterThanOrderByStockAsc(Integer threshold, Integer minimum);
    List<Product> findByDeletedAtIsNullAndStockOrderByNameAsc(Integer stock);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.deletedAt is null")
    Optional<Product> findForUpdateById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findAnyForUpdateById(@Param("id") Long id);
}
