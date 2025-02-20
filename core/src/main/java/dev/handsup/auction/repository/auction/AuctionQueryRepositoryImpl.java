package dev.handsup.auction.repository.auction;

import static dev.handsup.auction.domain.QAuction.*;
import static dev.handsup.auction.domain.product.QProduct.*;
import static dev.handsup.auction.domain.product.product_category.QProductCategory.*;
import static org.springframework.util.StringUtils.*;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import dev.handsup.auction.domain.Auction;
import dev.handsup.auction.domain.QAuction;
import dev.handsup.auction.domain.auction_field.AuctionStatus;
import dev.handsup.auction.domain.auction_field.TradeMethod;
import dev.handsup.auction.domain.product.ProductStatus;
import dev.handsup.auction.domain.product.product_category.ProductCategory;
import dev.handsup.auction.dto.request.AuctionSearchCondition;
import dev.handsup.auction.exception.AuctionErrorCode;
import dev.handsup.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuctionQueryRepositoryImpl implements AuctionQueryRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Slice<Auction> searchAuctions(AuctionSearchCondition condition, Pageable pageable) {
		List<Auction> content = queryFactory.select(QAuction.auction)
			.from(auction)
			.join(auction.product, product).fetchJoin()
			.leftJoin(product.productCategory, productCategory).fetchJoin()
			.where(
				keywordContains(condition.keyword()),
				categoryEq(condition.productCategory()),
				tradeMethodEq(condition.tradeMethod()),
				siEq(condition.si()),
				guEq(condition.gu()),
				dongEq(condition.dong()),
				initPriceMin(condition.minPrice()),
				initPriceMax(condition.maxPrice()),
				isNewProductEq(condition.isNewProduct()),
				isProgressEq(condition.isProgress())
			)
			.orderBy(searchAuctionSort(pageable))
			.limit(pageable.getPageSize() + 1L)
			.offset(pageable.getOffset())
			.fetch();
		boolean hasNext = hasNext(pageable.getPageSize(), content);
		return new SliceImpl<>(content, pageable, hasNext);
	}

	@Override
	public Slice<Auction> sortAuctionByCriteria(String si, String gu, String dong, Pageable pageable) {
		List<Auction> content = queryFactory.select(QAuction.auction)
			.from(auction)
			.join(auction.product, product).fetchJoin()
			.where(
				auction.status.eq(AuctionStatus.BIDDING),
				siEq(si),
				guEq(gu),
				dongEq(dong)
			)
			.orderBy(recommendAuctionSort(pageable))
			.limit(pageable.getPageSize() + 1L)
			.offset(pageable.getOffset())
			.fetch();
		boolean hasNext = hasNext(pageable.getPageSize(), content);
		return new SliceImpl<>(content, pageable, hasNext);
	}

	@Override
	public Slice<Auction> findByProductCategories(List<ProductCategory> productCategories, Pageable pageable) {
		List<Auction> content = queryFactory.select(QAuction.auction)
			.from(auction)
			.join(auction.product, product).fetchJoin()
			.where(
				auction.product.productCategory.in(productCategories)
			)
			.orderBy(auction.bookmarkCount.desc())
			.limit(pageable.getPageSize() + 1L)
			.offset(pageable.getOffset())
			.fetch();
		boolean hasNext = hasNext(pageable.getPageSize(), content);
		return new SliceImpl<>(content, pageable, hasNext);
	}

	@Override
	@Transactional
	public void updateAuctionStatusAfterEndDate() {
		queryFactory
			.update(auction)
			.set(auction.status, AuctionStatus.CANCELED)
			.where(auction.endDate.lt(LocalDate.now()),
				auction.biddingCount.eq(0))
			.execute();

		queryFactory
			.update(auction)
			.set(auction.status, AuctionStatus.TRADING)
			.where(auction.endDate.lt(LocalDate.now()),
				auction.biddingCount.goe(1))
			.execute();
	}

	private OrderSpecifier<?> searchAuctionSort(Pageable pageable) {
		return pageable.getSort().stream()
			.findFirst()
			.map(order -> switch (order.getProperty()) {
				case "북마크수" -> auction.bookmarkCount.desc();
				case "마감일" -> auction.endDate.asc();
				case "입찰수" -> auction.biddingCount.desc();
				default -> auction.createdAt.desc();
			})
			.orElse(auction.createdAt.desc()); // 기본값 최신순
	}

	private OrderSpecifier<?> recommendAuctionSort(Pageable pageable) {
		return pageable.getSort().stream()
			.findFirst()
			.map(order -> switch (order.getProperty()) {
				case "북마크수" -> auction.bookmarkCount.desc();
				case "마감일" -> auction.endDate.asc();
				case "입찰수" -> auction.biddingCount.desc();
				case "최근생성" -> auction.createdAt.desc();
				default -> throw new ValidationException(AuctionErrorCode.INVALID_SORT_INPUT); //기본값 비허용
			})
			.orElseThrow(() -> new ValidationException(AuctionErrorCode.EMPTY_SORT_INPUT)); //null 비허용
	}

	private BooleanExpression keywordContains(String keyword) {
		return keyword != null ? auction.title.contains(keyword) : null;
	}

	private BooleanExpression categoryEq(String productCategory) {
		return hasText(productCategory) ? product.productCategory.value.eq(productCategory) : null;
	}

	private BooleanExpression tradeMethodEq(String tradeMethod) {
		return hasText(tradeMethod) ? auction.tradeMethod.eq(TradeMethod.of(tradeMethod)) : null;
	}

	private BooleanExpression siEq(String si) {
		return hasText(si) ? auction.tradingLocation.si.eq(si) : null;
	}

	private BooleanExpression guEq(String gu) {
		return hasText(gu) ? auction.tradingLocation.gu.eq(gu) : null;
	}

	private BooleanExpression dongEq(String dong) {
		return hasText(dong) ? auction.tradingLocation.dong.eq(dong) : null;
	}

	private BooleanExpression initPriceMin(Integer minPrice) {
		return (minPrice != null) ? auction.initPrice.goe(minPrice) : null;
	}

	private BooleanExpression initPriceMax(Integer maxPrice) {
		return (maxPrice != null) ? auction.initPrice.loe(maxPrice) : null;
	}

	private BooleanExpression isNewProductEq(Boolean isNewProduct) {
		if (isNewProduct == null) {
			return null;
		}
		if (Boolean.TRUE.equals(isNewProduct)) {
			return auction.product.status.eq(ProductStatus.NEW);
		} else {
			return auction.product.status.eq(ProductStatus.CLEAN).or(auction.product.status.eq(ProductStatus.DIRTY));
		}
	}

	private BooleanExpression isProgressEq(Boolean isProgress) {
		if (Boolean.TRUE.equals(isProgress)) {
			return auction.status.eq(AuctionStatus.BIDDING);
		}
		return null;
	}

	private boolean hasNext(int pageSize, List<Auction> auctions) {
		if (auctions.size() <= pageSize) {
			return false;
		}
		auctions.remove(pageSize);
		return true;
	}
}
