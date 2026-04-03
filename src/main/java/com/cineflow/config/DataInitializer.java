package com.cineflow.config;

import com.cineflow.domain.Booking;
import com.cineflow.domain.BookingStatus;
import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.repository.BookingRepository;
import com.cineflow.repository.MovieRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(MovieRepository movieRepository, BookingRepository bookingRepository) {
        return args -> {
            if (movieRepository.count() == 0) {
                movieRepository.saveAll(List.of(
                        Movie.builder()
                                .title("시간의 궤도")
                                .shortDescription("우주 관측소에서 감지된 미확인 신호, 그리고 단 72시간.")
                                .description("지구 저궤도 관측소에서 발견된 정체불명의 신호. 임무 종료를 앞둔 팀은 그 신호의 정체가 인류의 생존과 연결되어 있다는 사실을 알게 되고, 제한된 시간 안에 선택을 내려야 한다.")
                                .genre("SF · 스릴러")
                                .ageRating("12")
                                .runningTime(132)
                                .posterUrl("/images/uploads/movie-single.jpg")
                                .bookingRate(31.2)
                                .score(9.1)
                                .releaseDate(LocalDate.of(2026, 4, 10))
                                .status(MovieStatus.NOW_SHOWING)
                                .build(),
                        Movie.builder()
                                .title("보이스 노이즈")
                                .shortDescription("도시를 뒤덮은 정체불명의 소음, 단서를 좇는 추적 스릴러.")
                                .description("정체를 알 수 없는 소음이 도시 전체를 마비시킨 밤. 사건 현장을 취재하던 기자가 감춰진 실험 기록과 연결된 비밀을 추적하기 시작한다.")
                                .genre("스릴러 · 미스터리")
                                .ageRating("15")
                                .runningTime(118)
                                .posterUrl("/images/uploads/mv-it2.jpg")
                                .bookingRate(24.8)
                                .score(8.7)
                                .releaseDate(LocalDate.of(2026, 4, 3))
                                .status(MovieStatus.NOW_SHOWING)
                                .build(),
                        Movie.builder()
                                .title("블랙아웃 시티")
                                .shortDescription("정전된 도시에서 벌어지는 하룻밤 생존 액션.")
                                .description("도시 전체가 암흑에 잠긴 밤, 서로 다른 목적을 가진 인물들이 한 지하 시설에 모이면서 예상치 못한 사건이 시작된다.")
                                .genre("액션 · 범죄")
                                .ageRating("15")
                                .runningTime(124)
                                .posterUrl("/images/uploads/mv-it5.jpg")
                                .bookingRate(19.7)
                                .score(8.5)
                                .releaseDate(LocalDate.of(2026, 3, 28))
                                .status(MovieStatus.NOW_SHOWING)
                                .build(),
                        Movie.builder()
                                .title("심해 항로")
                                .shortDescription("심해 기지에서 시작되는 미스터리 재난 드라마.")
                                .description("심해 탐사선이 구조 신호를 남긴 뒤 연락이 끊긴다. 구조팀이 현장에 도착하면서 예기치 못한 비밀과 마주하게 된다.")
                                .genre("미스터리 · 드라마")
                                .ageRating("12")
                                .runningTime(121)
                                .posterUrl("/images/uploads/mv-it6.jpg")
                                .bookingRate(14.2)
                                .score(8.2)
                                .releaseDate(LocalDate.of(2026, 3, 22))
                                .status(MovieStatus.NOW_SHOWING)
                                .build(),
                        Movie.builder()
                                .title("리버스 코드")
                                .shortDescription("시간의 단서를 조합해 진실을 추적하는 SF 미스터리.")
                                .description("미완성 신호를 추적하던 개발자가 사라진 기억의 조각 속에서 사건의 진실과 마주하게 되는 이야기.")
                                .genre("SF · 미스터리")
                                .ageRating("ALL")
                                .runningTime(116)
                                .posterUrl("/images/uploads/mv-it7.jpg")
                                .bookingRate(12.4)
                                .score(8.8)
                                .releaseDate(LocalDate.of(2026, 4, 18))
                                .status(MovieStatus.COMING_SOON)
                                .build(),
                        Movie.builder()
                                .title("극야의 기록")
                                .shortDescription("끝나지 않는 밤 속에 숨겨진 진실.")
                                .description("극지 연구기지에서 사라진 기록을 둘러싸고 벌어지는 심리 스릴러.")
                                .genre("스릴러")
                                .ageRating("19")
                                .runningTime(127)
                                .posterUrl("/images/uploads/mv-it8.jpg")
                                .bookingRate(8.1)
                                .score(8.1)
                                .releaseDate(LocalDate.of(2026, 4, 25))
                                .status(MovieStatus.COMING_SOON)
                                .build(),
                        Movie.builder()
                                .title("일리시스")
                                .shortDescription("잃어버린 좌표를 따라 떠나는 감성 로드무비.")
                                .description("오래된 무전기에서 흘러나온 메시지를 따라 두 사람이 긴 여정을 시작하는 감성 로드무비.")
                                .genre("드라마 · 어드벤처")
                                .ageRating("12")
                                .runningTime(111)
                                .posterUrl("/images/uploads/mv-it9.jpg")
                                .bookingRate(7.5)
                                .score(8.0)
                                .releaseDate(LocalDate.of(2026, 5, 1))
                                .status(MovieStatus.COMING_SOON)
                                .build()
                ));
            }

            if (bookingRepository.count() == 0) {
                bookingRepository.saveAll(List.of(
                        Booking.builder()
                                .bookingCode("CF20260410-1020-4H8K")
                                .customerName("김민서")
                                .customerPhone("010-1234-5678")
                                .movieTitle("시간의 궤도")
                                .posterUrl("/images/uploads/movie-single.jpg")
                                .ageRating("12")
                                .theaterName("CineFlow 강남")
                                .screenName("1관")
                                .screenType("IMAX")
                                .seatNames("E7, E8")
                                .peopleCount(2)
                                .totalPrice(29000)
                                .startTime(LocalDateTime.of(2026, 4, 10, 10, 20))
                                .endTime(LocalDateTime.of(2026, 4, 10, 12, 32))
                                .status(BookingStatus.BOOKED)
                                .build(),
                        Booking.builder()
                                .bookingCode("CF20260411-1940-T8M2")
                                .customerName("김민서")
                                .customerPhone("010-1234-5678")
                                .movieTitle("보이스 노이즈")
                                .posterUrl("/images/uploads/mv-it2.jpg")
                                .ageRating("15")
                                .theaterName("CineFlow 홍대")
                                .screenName("5관")
                                .screenType("2D")
                                .seatNames("H11, H12, H13")
                                .peopleCount(3)
                                .totalPrice(42000)
                                .startTime(LocalDateTime.of(2026, 4, 11, 19, 40))
                                .endTime(LocalDateTime.of(2026, 4, 11, 21, 38))
                                .status(BookingStatus.UPCOMING)
                                .build(),
                        Booking.builder()
                                .bookingCode("CF20260410-1310-A1Q9")
                                .customerName("김민서")
                                .customerPhone("010-1234-5678")
                                .movieTitle("극야의 기록")
                                .posterUrl("/images/uploads/mv-it8.jpg")
                                .ageRating("19")
                                .theaterName("CineFlow 용산")
                                .screenName("3관")
                                .screenType("LASER")
                                .seatNames("C4")
                                .peopleCount(1)
                                .totalPrice(15000)
                                .startTime(LocalDateTime.of(2026, 4, 10, 13, 10))
                                .endTime(LocalDateTime.of(2026, 4, 10, 15, 5))
                                .status(BookingStatus.SOON)
                                .build(),
                        Booking.builder()
                                .bookingCode("CF20260320-2100-F5F6")
                                .customerName("김민서")
                                .customerPhone("010-1234-5678")
                                .movieTitle("광야의 문")
                                .posterUrl("/images/uploads/mv-item6.jpg")
                                .ageRating("15")
                                .theaterName("CineFlow 강남")
                                .screenName("3관")
                                .screenType("LASER")
                                .seatNames("F5, F6")
                                .peopleCount(2)
                                .totalPrice(30000)
                                .startTime(LocalDateTime.of(2026, 3, 20, 21, 0))
                                .endTime(LocalDateTime.of(2026, 3, 20, 23, 5))
                                .status(BookingStatus.USED)
                                .build(),
                        Booking.builder()
                                .bookingCode("CF20260309-1830-G8")
                                .customerName("김민서")
                                .customerPhone("010-1234-5678")
                                .movieTitle("브루트: 라스트 미션")
                                .posterUrl("/images/uploads/mv-item4.jpg")
                                .ageRating("12")
                                .theaterName("CineFlow 홍대")
                                .screenName("2관")
                                .screenType("2D")
                                .seatNames("G8")
                                .peopleCount(1)
                                .totalPrice(14000)
                                .startTime(LocalDateTime.of(2026, 3, 9, 18, 30))
                                .endTime(LocalDateTime.of(2026, 3, 9, 20, 12))
                                .status(BookingStatus.USED)
                                .build()
                ));
            }
        };
    }
}