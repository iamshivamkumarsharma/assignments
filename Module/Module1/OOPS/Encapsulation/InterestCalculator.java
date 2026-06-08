package InterestCalculator;





    public class InterestCalculator {

        private double principal;
        private double rate;
        private int years;

        public InterestCalculator(double principal,
                                  double rate,
                                  int years) {

            this.principal = principal;
            this.rate = rate;
            this.years = years;
        }

        public double getPrincipal() {
            return principal;
        }

        public double getRate() {
            return rate;
        }

        public int getYears() {
            return years;
        }

        public double calculateSimpleInterest() {
            return (principal * rate * years) / 100;
        }

        public double totalAmount() {
            return principal + calculateSimpleInterest();
        }
    }


