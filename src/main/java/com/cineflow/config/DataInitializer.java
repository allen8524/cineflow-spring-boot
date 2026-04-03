package com.cineflow.config;

import com.cineflow.domain.Booking;
import com.cineflow.domain.BookingSeat;
import com.cineflow.domain.BookingStatus;
import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.domain.Payment;
import com.cineflow.domain.PaymentMethod;
import com.cineflow.domain.PaymentStatus;
import com.cineflow.domain.Schedule;
import com.cineflow.domain.ScheduleSeat;
import com.cineflow.domain.Screen;
import com.cineflow.domain.SeatTemplate;
import com.cineflow.domain.SeatType;
import com.cineflow.domain.Theater;
import com.cineflow.repository.BookingRepository;
import com.cineflow.repository.BookingSeatRepository;
import com.cineflow.repository.MovieRepository;
import com.cineflow.repository.PaymentRepository;
import com.cineflow.repository.ScheduleRepository;
import com.cineflow.repository.ScheduleSeatRepository;
import com.cineflow.repository.ScreenRepository;
import com.cineflow.repository.SeatTemplateRepository;
import com.cineflow.repository.TheaterRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
    CommandLineRunner initData(
            MovieRepository movieRepository,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            TheaterRepository theaterRepository,
            ScreenRepository screenRepository,
            ScheduleRepository scheduleRepository,
            SeatTemplateRepository seatTemplateRepository,
            ScheduleSeatRepository scheduleSeatRepository
    ) {
        return args -> {
            initializeMovies(movieRepository);
            initializeCinemaStructure(movieRepository, theaterRepository, screenRepository, scheduleRepository);
            initializeSeatTemplates(screenRepository, seatTemplateRepository);
            initializeScheduleSeats(scheduleRepository, seatTemplateRepository, scheduleSeatRepository);
            initializeSeedBookings(bookingRepository, bookingSeatRepository, paymentRepository, scheduleRepository, scheduleSeatRepository);
            syncScheduleAvailability(scheduleRepository, scheduleSeatRepository);
        };
    }

    private void initializeMovies(MovieRepository movieRepository) {
        if (movieRepository.count() > 0) {
            return;
        }

        movieRepository.saveAll(List.of(
                Movie.builder()
                        .title("시간의 궤도")
                        .shortDescription("우주 관측소에서 감지된 미확인 신호, 그리고 남겨진 72시간.")
                        .description("지구 저궤도 관측소에서 발견된 정체불명의 신호. 임무 종료를 앞둔 탐사팀은 그 신호가 인류의 생존과 연결되어 있다는 사실을 알게 되고, 제한된 시간 안에 선택을 내려야 한다.")
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
                        .description("정체를 알 수 없는 소음이 도시 전체를 마비시킨 밤. 사건 현장을 취재하던 기자가 감춰진 기록과 연결된 비밀을 추적하기 시작한다.")
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
                        .shortDescription("정전된 도시에서 벌어지는 하룻밤의 생존 액션.")
                        .description("도시 전체가 정전된 밤, 서로 다른 목적을 가진 인물들이 한 건물에 모이면서 예상치 못한 사건이 시작된다.")
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
                        .description("심해 연구기지에서 구조 신호를 포착한 탐사팀이 현장에 도착하면서 예상치 못한 비밀과 마주하게 된다.")
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
                        .shortDescription("시간과 기억의 단서를 조합해 진실을 쫓는 SF 미스터리.")
                        .description("사라진 기억 조각을 되짚는 개발자가 의문의 코드와 연결된 사건의 진실과 마주하게 되는 이야기.")
                        .genre("SF · 미스터리")
                        .ageRating("12")
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
                        .description("오래된 무전기에서 흘러나온 메시지를 따라 두 사람이 길 위에서 새로운 여정을 시작하는 감성 로드무비.")
                        .genre("드라마 · 어드벤처")
                        .ageRating("ALL")
                        .runningTime(111)
                        .posterUrl("/images/uploads/mv-it9.jpg")
                        .bookingRate(7.5)
                        .score(8.0)
                        .releaseDate(LocalDate.of(2026, 5, 1))
                        .status(MovieStatus.COMING_SOON)
                        .build()
        ));
    }

    private void initializeCinemaStructure(
            MovieRepository movieRepository,
            TheaterRepository theaterRepository,
            ScreenRepository screenRepository,
            ScheduleRepository scheduleRepository
    ) {
        if (theaterRepository.count() > 0 || screenRepository.count() > 0 || scheduleRepository.count() > 0) {
            return;
        }

        List<Movie> movies = movieRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (movies.size() < 5) {
            throw new IllegalStateException("상영시간표를 만들기 위한 영화 데이터가 충분하지 않습니다.");
        }

        Movie timeOrbit = movies.get(0);
        Movie voiceNoise = movies.get(1);
        Movie blackoutCity = movies.get(2);
        Movie deepSeaRoute = movies.get(3);
        Movie reverseCode = movies.get(4);

        Theater gangnam = theaterRepository.save(Theater.builder()
                .name("CineFlow 강남")
                .location("서울특별시 강남구 테헤란로 410")
                .region("서울")
                .description("프리미엄 IMAX와 심야 상영이 강점인 대표 지점")
                .build());

        Theater hongdae = theaterRepository.save(Theater.builder()
                .name("CineFlow 홍대")
                .location("서울특별시 마포구 양화로 176")
                .region("서울")
                .description("젊은 관객층이 많은 도심형 멀티플렉스")
                .build());

        Theater jamsil = theaterRepository.save(Theater.builder()
                .name("CineFlow 잠실")
                .location("서울특별시 송파구 올림픽로 300")
                .region("서울")
                .description("돌비 사운드와 가족 관람 수요가 많은 복합관")
                .build());

        Screen gangnamImax = screenRepository.save(Screen.builder()
                .theater(gangnam)
                .name("1관")
                .screenType("IMAX")
                .totalSeats(120)
                .build());

        Screen gangnamStandard = screenRepository.save(Screen.builder()
                .theater(gangnam)
                .name("2관")
                .screenType("2D")
                .totalSeats(120)
                .build());

        Screen hongdaeLaser = screenRepository.save(Screen.builder()
                .theater(hongdae)
                .name("3관")
                .screenType("LASER")
                .totalSeats(120)
                .build());

        Screen hongdaeStandard = screenRepository.save(Screen.builder()
                .theater(hongdae)
                .name("5관")
                .screenType("2D")
                .totalSeats(120)
                .build());

        Screen jamsil4dx = screenRepository.save(Screen.builder()
                .theater(jamsil)
                .name("6관")
                .screenType("4DX")
                .totalSeats(120)
                .build());

        Screen jamsilDolby = screenRepository.save(Screen.builder()
                .theater(jamsil)
                .name("8관")
                .screenType("DOLBY ATMOS")
                .totalSeats(120)
                .build());

        scheduleRepository.saveAll(List.of(
                createSchedule(timeOrbit, gangnamImax, LocalDateTime.of(2026, 4, 3, 10, 20), 22000, 58),
                createSchedule(timeOrbit, hongdaeLaser, LocalDateTime.of(2026, 4, 4, 14, 5), 18000, 73),
                createSchedule(timeOrbit, jamsilDolby, LocalDateTime.of(2026, 4, 5, 19, 40), 19000, 44),

                createSchedule(voiceNoise, hongdaeStandard, LocalDateTime.of(2026, 4, 3, 12, 45), 15000, 81),
                createSchedule(voiceNoise, gangnamStandard, LocalDateTime.of(2026, 4, 4, 19, 40), 15000, 67),
                createSchedule(voiceNoise, jamsilDolby, LocalDateTime.of(2026, 4, 6, 17, 20), 19000, 35),

                createSchedule(blackoutCity, jamsil4dx, LocalDateTime.of(2026, 4, 3, 15, 30), 23000, 29),
                createSchedule(blackoutCity, hongdaeLaser, LocalDateTime.of(2026, 4, 5, 20, 10), 18000, 51),

                createSchedule(deepSeaRoute, gangnamStandard, LocalDateTime.of(2026, 4, 4, 9, 30), 15000, 92),
                createSchedule(deepSeaRoute, hongdaeStandard, LocalDateTime.of(2026, 4, 6, 13, 5), 15000, 63),

                createSchedule(reverseCode, jamsilDolby, LocalDateTime.of(2026, 4, 18, 18, 30), 19000, 120),
                createSchedule(reverseCode, gangnamImax, LocalDateTime.of(2026, 4, 19, 11, 0), 22000, 101)
        ));
    }

    private void initializeSeatTemplates(ScreenRepository screenRepository, SeatTemplateRepository seatTemplateRepository) {
        if (seatTemplateRepository.count() > 0) {
            return;
        }

        List<SeatTemplate> seatTemplates = new ArrayList<>();
        for (Screen screen : screenRepository.findAll()) {
            for (char row = 'A'; row <= 'J'; row++) {
                for (int number = 1; number <= 12; number++) {
                    seatTemplates.add(SeatTemplate.builder()
                            .screen(screen)
                            .seatRow(String.valueOf(row))
                            .seatNumber(number)
                            .seatCode(row + String.valueOf(number))
                            .seatType(resolveSeatType(row, number))
                            .active(true)
                            .build());
                }
            }
        }

        seatTemplateRepository.saveAll(seatTemplates);
    }

    private void initializeScheduleSeats(
            ScheduleRepository scheduleRepository,
            SeatTemplateRepository seatTemplateRepository,
            ScheduleSeatRepository scheduleSeatRepository
    ) {
        if (scheduleSeatRepository.count() > 0) {
            return;
        }

        List<ScheduleSeat> scheduleSeats = new ArrayList<>();
        for (Schedule schedule : scheduleRepository.findByActiveTrueOrderByStartTimeAsc()) {
            List<SeatTemplate> seatTemplates = seatTemplateRepository.findByScreenIdAndActiveTrueOrderBySeatRowAscSeatNumberAsc(
                    schedule.getScreen().getId()
            );

            for (SeatTemplate seatTemplate : seatTemplates) {
                scheduleSeats.add(ScheduleSeat.builder()
                        .schedule(schedule)
                        .seatTemplate(seatTemplate)
                        .reserved(false)
                        .held(false)
                        .priceOverride(resolveSeatPrice(schedule.getPrice(), seatTemplate.getSeatType()))
                        .build());
            }
        }

        scheduleSeatRepository.saveAll(scheduleSeats);
    }

    private void initializeSeedBookings(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            ScheduleRepository scheduleRepository,
            ScheduleSeatRepository scheduleSeatRepository
    ) {
        if (bookingRepository.count() > 0 || paymentRepository.count() > 0) {
            return;
        }

        List<Schedule> schedules = scheduleRepository.findByActiveTrueOrderByStartTimeAsc();

        Schedule timeOrbitGangnam = findSchedule(schedules, "시간의 궤도", LocalDateTime.of(2026, 4, 3, 10, 20));
        Schedule timeOrbitJamsil = findSchedule(schedules, "시간의 궤도", LocalDateTime.of(2026, 4, 5, 19, 40));
        Schedule voiceNoiseGangnam = findSchedule(schedules, "보이스 노이즈", LocalDateTime.of(2026, 4, 4, 19, 40));

        createBookingWithSeats(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                scheduleSeatRepository,
                timeOrbitGangnam,
                "CF20260403-1020-4H8K",
                "김민서",
                "010-1234-5678",
                List.of("E7", "E8"),
                BookingStatus.BOOKED,
                PaymentMethod.CARD,
                44000
        );

        createBookingWithSeats(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                scheduleSeatRepository,
                voiceNoiseGangnam,
                "CF20260404-1940-T8M2",
                "김민서",
                "010-1234-5678",
                List.of("H10", "H11", "H12"),
                BookingStatus.BOOKED,
                PaymentMethod.KAKAO_PAY,
                45000
        );

        createBookingWithSeats(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                scheduleSeatRepository,
                timeOrbitJamsil,
                "CF20260405-1940-A1Q9",
                "김민서",
                "010-1234-5678",
                List.of("C4"),
                BookingStatus.BOOKED,
                PaymentMethod.NAVER_PAY,
                19000
        );

        createStandaloneBooking(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                "CF20260320-2100-F5F6",
                "블랙아웃 시티",
                "/images/uploads/mv-it5.jpg",
                "15",
                "CineFlow 잠실",
                "6관",
                "4DX",
                List.of("F5", "F6"),
                2,
                46000,
                LocalDateTime.of(2026, 3, 20, 21, 0),
                LocalDateTime.of(2026, 3, 20, 23, 4),
                BookingStatus.USED,
                PaymentMethod.CARD
        );

        createStandaloneBooking(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                "CF20260309-1830-G8",
                "심해 항로",
                "/images/uploads/mv-it6.jpg",
                "12",
                "CineFlow 홍대",
                "5관",
                "2D",
                List.of("G8"),
                1,
                15000,
                LocalDateTime.of(2026, 3, 9, 18, 30),
                LocalDateTime.of(2026, 3, 9, 20, 31),
                BookingStatus.USED,
                PaymentMethod.TOSS
        );

        createCanceledBooking(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                "CF20260402-1810-CN01",
                "취소된 예매 샘플",
                "/images/uploads/movie-single.jpg",
                "12",
                "CineFlow 강남",
                "2관",
                "2D",
                List.of("D5", "D6"),
                2,
                30000,
                LocalDateTime.of(2026, 4, 2, 18, 10),
                LocalDateTime.of(2026, 4, 2, 20, 8),
                "일정 변경"
        );

        for (Schedule schedule : schedules) {
            reserveAdditionalSeatsToTarget(schedule, schedule.getAvailableSeats(), scheduleSeatRepository);
        }
    }

    private void syncScheduleAvailability(ScheduleRepository scheduleRepository, ScheduleSeatRepository scheduleSeatRepository) {
        List<Schedule> schedules = scheduleRepository.findByActiveTrueOrderByStartTimeAsc();
        for (Schedule schedule : schedules) {
            schedule.setAvailableSeats((int) scheduleSeatRepository.countByScheduleIdAndReservedFalse(schedule.getId()));
        }
        scheduleRepository.saveAll(schedules);
    }

    private Schedule createSchedule(Movie movie, Screen screen, LocalDateTime startTime, int price, int availableSeats) {
        return Schedule.builder()
                .movie(movie)
                .screen(screen)
                .startTime(startTime)
                .endTime(startTime.plusMinutes(movie.getRunningTime()))
                .price(price)
                .availableSeats(availableSeats)
                .active(true)
                .build();
    }

    private SeatType resolveSeatType(char row, int number) {
        if (row == 'A' && number >= 5 && number <= 8) {
            return SeatType.PREMIUM;
        }
        if (row == 'J' && number >= 4 && number <= 9) {
            return SeatType.COUPLE;
        }
        return SeatType.STANDARD;
    }

    private int resolveSeatPrice(int schedulePrice, SeatType seatType) {
        return switch (seatType) {
            case PREMIUM -> schedulePrice + 3000;
            case COUPLE -> schedulePrice + 5000;
            case STANDARD -> schedulePrice;
        };
    }

    private Schedule findSchedule(List<Schedule> schedules, String title, LocalDateTime startTime) {
        return schedules.stream()
                .filter(schedule -> schedule.getMovie().getTitle().equals(title))
                .filter(schedule -> schedule.getStartTime().equals(startTime))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기준 상영시간표를 찾을 수 없습니다. title=" + title + ", startTime=" + startTime));
    }

    private void createBookingWithSeats(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            ScheduleSeatRepository scheduleSeatRepository,
            Schedule schedule,
            String bookingCode,
            String customerName,
            String customerPhone,
            List<String> seatCodes,
            BookingStatus status,
            PaymentMethod paymentMethod,
            int totalPrice
    ) {
        List<ScheduleSeat> selectedSeats = scheduleSeatRepository.findByScheduleIdAndSeatTemplateSeatCodeIn(schedule.getId(), seatCodes);
        selectedSeats.forEach(scheduleSeat -> scheduleSeat.setReserved(true));
        scheduleSeatRepository.saveAll(selectedSeats);

        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode(bookingCode)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .movieTitle(schedule.getMovie().getTitle())
                .posterUrl(schedule.getMovie().getPosterUrl())
                .ageRating(schedule.getMovie().getAgeRating())
                .theaterName(schedule.getScreen().getTheater().getName())
                .screenName(schedule.getScreen().getName())
                .screenType(schedule.getScreen().getScreenType())
                .seatNames(String.join(", ", seatCodes))
                .peopleCount(seatCodes.size())
                .totalPrice(totalPrice)
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .schedule(schedule)
                .status(status)
                .build());

        bookingSeatRepository.saveAll(selectedSeats.stream()
                .map(scheduleSeat -> BookingSeat.builder()
                        .booking(booking)
                        .seatCode(scheduleSeat.getSeatTemplate().getSeatCode())
                        .seatRow(scheduleSeat.getSeatTemplate().getSeatRow())
                        .seatNumber(scheduleSeat.getSeatTemplate().getSeatNumber())
                        .seatType(scheduleSeat.getSeatTemplate().getSeatType())
                        .price(scheduleSeat.getPriceOverride())
                        .build())
                .toList());

        paymentRepository.save(Payment.builder()
                .booking(booking)
                .method(paymentMethod)
                .amount(totalPrice)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(schedule.getStartTime().minusHours(2))
                .transactionId("SEED-" + bookingCode.substring(Math.max(0, bookingCode.length() - 8)).toUpperCase(Locale.ROOT))
                .build());
    }

    private void createStandaloneBooking(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            String bookingCode,
            String movieTitle,
            String posterUrl,
            String ageRating,
            String theaterName,
            String screenName,
            String screenType,
            List<String> seatCodes,
            int peopleCount,
            int totalPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BookingStatus status,
            PaymentMethod paymentMethod
    ) {
        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode(bookingCode)
                .customerName("김민서")
                .customerPhone("010-1234-5678")
                .movieTitle(movieTitle)
                .posterUrl(posterUrl)
                .ageRating(ageRating)
                .theaterName(theaterName)
                .screenName(screenName)
                .screenType(screenType)
                .seatNames(String.join(", ", seatCodes))
                .peopleCount(peopleCount)
                .totalPrice(totalPrice)
                .startTime(startTime)
                .endTime(endTime)
                .status(status)
                .build());

        int seatPrice = totalPrice / Math.max(1, seatCodes.size());
        bookingSeatRepository.saveAll(seatCodes.stream()
                .map(this::parseSeatCode)
                .map(parsedSeat -> BookingSeat.builder()
                        .booking(booking)
                        .seatCode(parsedSeat.seatCode())
                        .seatRow(parsedSeat.seatRow())
                        .seatNumber(parsedSeat.seatNumber())
                        .seatType(SeatType.STANDARD)
                        .price(seatPrice)
                        .build())
                .toList());

        paymentRepository.save(Payment.builder()
                .booking(booking)
                .method(paymentMethod)
                .amount(totalPrice)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(startTime.minusHours(3))
                .transactionId("SEED-" + bookingCode.substring(Math.max(0, bookingCode.length() - 8)).toUpperCase(Locale.ROOT))
                .build());
    }

    private void createCanceledBooking(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            String bookingCode,
            String movieTitle,
            String posterUrl,
            String ageRating,
            String theaterName,
            String screenName,
            String screenType,
            List<String> seatCodes,
            int peopleCount,
            int totalPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String cancelReason
    ) {
        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode(bookingCode)
                .customerName("관리 샘플")
                .customerPhone("010-9999-0000")
                .movieTitle(movieTitle)
                .posterUrl(posterUrl)
                .ageRating(ageRating)
                .theaterName(theaterName)
                .screenName(screenName)
                .screenType(screenType)
                .seatNames(String.join(", ", seatCodes))
                .peopleCount(peopleCount)
                .totalPrice(totalPrice)
                .startTime(startTime)
                .endTime(endTime)
                .status(BookingStatus.CANCELED)
                .cancelReason(cancelReason)
                .canceledAt(startTime.minusDays(1))
                .build());

        int seatPrice = totalPrice / Math.max(1, seatCodes.size());
        bookingSeatRepository.saveAll(seatCodes.stream()
                .map(this::parseSeatCode)
                .map(parsedSeat -> BookingSeat.builder()
                        .booking(booking)
                        .seatCode(parsedSeat.seatCode())
                        .seatRow(parsedSeat.seatRow())
                        .seatNumber(parsedSeat.seatNumber())
                        .seatType(SeatType.STANDARD)
                        .price(seatPrice)
                        .build())
                .toList());

        paymentRepository.save(Payment.builder()
                .booking(booking)
                .method(PaymentMethod.CARD)
                .amount(totalPrice)
                .paymentStatus(PaymentStatus.CANCELED)
                .paidAt(startTime.minusDays(2))
                .canceledAt(startTime.minusDays(1))
                .transactionId("SEED-" + bookingCode.substring(Math.max(0, bookingCode.length() - 8)).toUpperCase(Locale.ROOT))
                .cancelTransactionId("CANCEL-" + bookingCode.substring(Math.max(0, bookingCode.length() - 6)).toUpperCase(Locale.ROOT))
                .build());
    }

    private void reserveAdditionalSeatsToTarget(
            Schedule schedule,
            int targetAvailableSeats,
            ScheduleSeatRepository scheduleSeatRepository
    ) {
        List<ScheduleSeat> seats = scheduleSeatRepository.findByScheduleIdOrderBySeatTemplateSeatRowAscSeatTemplateSeatNumberAsc(schedule.getId());
        int safeTargetAvailable = Math.max(0, Math.min(targetAvailableSeats, seats.size()));
        int targetReservedCount = seats.size() - safeTargetAvailable;
        int currentReservedCount = (int) seats.stream().filter(ScheduleSeat::isReserved).count();

        if (currentReservedCount >= targetReservedCount) {
            return;
        }

        seats.stream()
                .filter(scheduleSeat -> !scheduleSeat.isReserved())
                .sorted(Comparator
                        .comparing((ScheduleSeat scheduleSeat) -> scheduleSeat.getSeatTemplate().getSeatRow())
                        .thenComparing(scheduleSeat -> scheduleSeat.getSeatTemplate().getSeatNumber()))
                .limit(targetReservedCount - currentReservedCount)
                .forEach(scheduleSeat -> scheduleSeat.setReserved(true));

        scheduleSeatRepository.saveAll(seats);
    }

    private ParsedSeat parseSeatCode(String seatCode) {
        String seatRow = seatCode.replaceAll("\\d", "");
        int seatNumber = Integer.parseInt(seatCode.replaceAll("\\D", ""));
        return new ParsedSeat(seatCode, seatRow, seatNumber);
    }

    private record ParsedSeat(String seatCode, String seatRow, int seatNumber) {
    }
}
