
import java.util.*;

public class WaypointDemo {

    record CustomerId(int id) {}
    record BookingId(int id) {}
    record SpotId(int id) {}
    record Money(long paise, String currency) {
        Money {
            if (paise < 0 || currency == null || currency.isBlank())
                throw new IllegalArgumentException("Invalid money");
        }
        Money times(int hours) {
            return new Money(paise * hours, currency);
        }
        public String toString() {
            return currency + " " + paise / 100.0;
        }
    }

    static class ParkingSpot {
        SpotId id;
        String location, vehicle;
        ParkingSpot(SpotId id, String location) {
            this.id = id;
            this.location = location;
        }
        boolean available() {
            return vehicle == null;
        }
        void park(String v) {
            if (!available()) throw new IllegalStateException("Occupied");
            vehicle = v;
        }
        void release() {
            if (available()) throw new IllegalStateException("Already free");
            vehicle = null;
        }
    }

    static class Booking {
        BookingId id;
        CustomerId customer;
        SpotId spot;
        Money fare;
        String status = "PENDING";

        Booking(BookingId id, CustomerId c, SpotId s, Money fare) {
            this.id = id;
            customer = c;
            spot = s;
            this.fare = fare;
        }
        void confirm() {
            if (!status.equals("PENDING"))
                throw new IllegalStateException("Invalid booking");
            status = "CONFIRMED";
        }
        void cancel() {
            if (!status.equals("CONFIRMED"))
                throw new IllegalStateException("Cannot cancel");
            status = "CANCELLED";
        }
    }

    static class Waypoint {
        Map<CustomerId, String> customers = new HashMap<>();
        Map<SpotId, ParkingSpot> spots = new HashMap<>();
        Map<BookingId, Booking> bookings = new HashMap<>();
        Set<String> vehicles = new HashSet<>();
        int nextBooking = 1;

        void addCustomer(CustomerId id, String name) {
            customers.put(id, name);
        }

        void addSpot(ParkingSpot spot) {
            spots.put(spot.id, spot);
        }

        void addVehicle(String vehicle) {
            if (!vehicles.add(vehicle))
                throw new IllegalArgumentException("Duplicate vehicle");
        }

        List<ParkingSpot> getSpots() {
            return List.copyOf(spots.values());
        }

        Booking reserve(CustomerId customer, SpotId spot,
                        String vehicle, int hours, Money rate) {

            if (!customers.containsKey(customer))
                throw new IllegalArgumentException("Customer not found");

            if (!vehicles.contains(vehicle))
                throw new IllegalArgumentException("Vehicle not registered");

            ParkingSpot p = spots.get(spot);

            if (p == null || !p.available())
                throw new IllegalStateException("Spot unavailable");

            if (hours <= 0)
                throw new IllegalArgumentException("Invalid hours");

            p.park(vehicle);

            Booking b = new Booking(
                new BookingId(nextBooking++),
                customer, spot, rate.times(hours)
            );

            b.confirm();
            bookings.put(b.id, b);
            return b;
        }

        void cancel(BookingId id) {
            Booking b = bookings.get(id);
            if (b == null) throw new IllegalArgumentException("Not found");
            b.cancel();
            spots.get(b.spot).release();
        }
    }

    public static void main(String[] args) {

        Waypoint w = new Waypoint();

        CustomerId c = new CustomerId(101);
        SpotId s = new SpotId(201);

        w.addCustomer(c, "Rishik");
        w.addSpot(new ParkingSpot(s, "Bachupally"));
        w.addVehicle("TS09AB1233");

        System.out.println("Customer registered");
        System.out.println("Parking spot added");

        Booking b = w.reserve(
            c, s, "TS09AB1233", 2,
            new Money(5000, "INR")
        );

        System.out.println("Booking ID: " + b.id.id());
        System.out.println("Fare: " + b.fare);
        System.out.println("Status: " + b.status);

        try {
            w.reserve(c, s, "TS09AB1233", 1,
                      new Money(5000, "INR"));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        w.cancel(b.id);

        System.out.println("After cancellation: " + b.status);
        System.out.println("Spot available: " +
                           w.getSpots().get(0).available());
    }
}
