class JavaVersion {
    public static void main(String[] args) {
        switch (args[0].toLowerCase()) {
            case "-version":
                System.out.print(System.getProperty("java.version"));
                break;
            case "-vendor":
                System.out.print(System.getProperty("java.vendor"));
                break;
            case "-vmname":
                System.out.print(System.getProperty("java.vm.name"));
                break;
            case "-vminfo":
                System.out.print(System.getProperty("java.vm.info"));
                break;
            default:
                System.out.print("JavaVersion -version|-vendor|-vmname|-vminfo");
        }
    }
}
