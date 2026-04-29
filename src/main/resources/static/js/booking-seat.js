document.addEventListener("DOMContentLoaded", function () {
    var form = document.getElementById("seat-selection-form");
    if (!form) {
        return;
    }

    var seatButtons = Array.from(document.querySelectorAll(".seat-map .seat[data-seat-code]"));
    var selectedSeatNames = document.getElementById("selected-seat-names");
    var selectedPeopleLabel = document.getElementById("selected-people-label");
    var selectedTotalPrice = document.getElementById("selected-total-price");
    var selectedSeatInputs = document.getElementById("selected-seat-inputs");
    var peopleCountNote = document.getElementById("people-count-note");
    var selectedSeatCount = document.getElementById("selected-seat-count");
    var summaryAmount = document.getElementById("summary-amount");
    var resetButton = document.getElementById("reset-seat-selection");

    var countInputs = {
        adult: document.getElementById("adult-count-input"),
        teen: document.getElementById("teen-count-input"),
        senior: document.getElementById("senior-count-input")
    };

    var selectedSeats = [];

    function getInitialSeatCodes() {
        return Array.from(selectedSeatInputs.querySelectorAll('input[name="seatCodes"]'))
            .map(function (input) {
                return (input.value || "").trim().toUpperCase();
            })
            .filter(function (seatCode) {
                return seatCode.length > 0;
            });
    }

    function getCount(type) {
        return parseInt(countInputs[type].value || "0", 10);
    }

    function setCount(type, value) {
        countInputs[type].value = Math.max(0, value);
    }

    function getPeopleCount() {
        return getCount("adult") + getCount("teen") + getCount("senior");
    }

    function formatCurrency(value) {
        return value.toLocaleString("ko-KR") + "원";
    }

    function renderSeatInputs() {
        selectedSeatInputs.innerHTML = "";
        selectedSeats.forEach(function (seat) {
            var input = document.createElement("input");
            input.type = "hidden";
            input.name = "seatCodes";
            input.value = seat.code;
            selectedSeatInputs.appendChild(input);
        });
    }

    function renderCounters() {
        document.querySelectorAll(".people-counter").forEach(function (counter) {
            var type = counter.dataset.target;
            var display = counter.querySelector("[data-count-display]");
            display.textContent = String(getCount(type));
        });
    }

    function trimSelectedSeatsToPeopleCount() {
        while (selectedSeats.length > getPeopleCount()) {
            var removed = selectedSeats.pop();
            var button = seatButtons.find(function (seatButton) {
                return seatButton.dataset.seatCode === removed.code;
            });
            if (button) {
                button.classList.remove("selected");
            }
        }
    }

    function renderSummary() {
        var totalPeople = getPeopleCount();
        var totalPrice = selectedSeats.reduce(function (sum, seat) {
            return sum + seat.price;
        }, 0);

        selectedSeatNames.textContent = selectedSeats.length > 0
            ? selectedSeats.map(function (seat) { return seat.code; }).join(", ")
            : "선택 없음";
        selectedPeopleLabel.textContent = "성인 " + getCount("adult")
            + "명 / 청소년 " + getCount("teen")
            + "명 / 우대 " + getCount("senior") + "명";
        selectedTotalPrice.textContent = formatCurrency(totalPrice);
        summaryAmount.textContent = formatCurrency(totalPrice);
        peopleCountNote.textContent = "총 " + totalPeople + "명 | 선택 좌석 " + selectedSeats.length + "석";
        selectedSeatCount.textContent = selectedSeats.length > 0 ? selectedSeats.length + "석 선택" : "좌석 미선택";

        renderSeatInputs();
        renderCounters();
    }

    function findSelectedSeatIndex(seatCode) {
        return selectedSeats.findIndex(function (seat) {
            return seat.code === seatCode;
        });
    }

    function compareSeatCodes(leftCode, rightCode) {
        var leftRow = leftCode.replace(/[0-9]/g, "");
        var rightRow = rightCode.replace(/[0-9]/g, "");
        if (leftRow !== rightRow) {
            return leftRow.localeCompare(rightRow, "ko");
        }
        return parseInt(leftCode.replace(/\D/g, ""), 10) - parseInt(rightCode.replace(/\D/g, ""), 10);
    }

    function toggleSeatSelection(button) {
        var seatCode = button.dataset.seatCode;
        var seatPrice = parseInt(button.dataset.seatPrice || "0", 10);
        var index = findSelectedSeatIndex(seatCode);

        if (index >= 0) {
            selectedSeats.splice(index, 1);
            button.classList.remove("selected");
            renderSummary();
            return;
        }

        if (selectedSeats.length >= getPeopleCount()) {
            window.alert("선택한 인원 수만큼만 좌석을 선택할 수 있습니다.");
            return;
        }

        selectedSeats.push({code: seatCode, price: seatPrice});
        selectedSeats.sort(function (left, right) {
            return compareSeatCodes(left.code, right.code);
        });
        button.classList.add("selected");
        renderSummary();
    }

    function hydrateInitialSeatSelection() {
        var initialSeatCodes = getInitialSeatCodes();

        if (initialSeatCodes.length === 0) {
            return;
        }

        initialSeatCodes.forEach(function (seatCode) {
            var button = seatButtons.find(function (seatButton) {
                return seatButton.dataset.seatCode === seatCode && !seatButton.disabled;
            });

            if (!button || findSelectedSeatIndex(seatCode) >= 0) {
                return;
            }

            selectedSeats.push({
                code: seatCode,
                price: parseInt(button.dataset.seatPrice || "0", 10)
            });
            button.classList.add("selected");
        });

        selectedSeats.sort(function (left, right) {
            return compareSeatCodes(left.code, right.code);
        });
        trimSelectedSeatsToPeopleCount();
    }

    document.querySelectorAll(".people-counter button").forEach(function (button) {
        button.addEventListener("click", function () {
            var counter = button.closest(".people-counter");
            var type = counter.dataset.target;
            var action = button.dataset.action;
            var nextValue = getCount(type) + (action === "increase" ? 1 : -1);
            setCount(type, nextValue);
            trimSelectedSeatsToPeopleCount();
            renderSummary();
        });
    });

    seatButtons.forEach(function (button) {
        if (button.disabled) {
            return;
        }
        button.addEventListener("click", function () {
            toggleSeatSelection(button);
        });
    });

    if (resetButton) {
        resetButton.addEventListener("click", function (event) {
            event.preventDefault();
            selectedSeats = [];
            seatButtons.forEach(function (button) {
                button.classList.remove("selected");
            });
            renderSummary();
        });
    }

    form.addEventListener("submit", function (event) {
        if (getPeopleCount() <= 0) {
            event.preventDefault();
            window.alert("관람 인원은 1명 이상 선택해 주세요.");
            return;
        }

        if (selectedSeats.length !== getPeopleCount()) {
            event.preventDefault();
            window.alert("선택한 좌석 수와 관람 인원이 일치해야 합니다.");
        }
    });

    hydrateInitialSeatSelection();
    renderSummary();
});
