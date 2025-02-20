package dev.handsup.common.domain;

import lombok.Getter;

@Getter
public enum CacheType {

	AUCTIONS(
		"auctions",        // 캐시 이름: users
		10 * 60,                    // 만료 시간: 5 분
		10000                        // 최대 갯수: 10000
	);

	private final String cacheName;
	private final int expireAfterWrite;
	private final int maximumSize;

	CacheType(
		String cacheName,
		int expireSecondsAfterWrite,
		int maximumSize
	) {
		this.cacheName = cacheName;
		this.expireAfterWrite = expireSecondsAfterWrite;
		this.maximumSize = maximumSize;
	}
}
