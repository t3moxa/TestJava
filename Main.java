import com.jcraft.jsch.*;
import java.util.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите адрес сервера: ");
        String address = scanner.nextLine();
        System.out.print("Введите порт сервера: ");
        int port = Integer.parseInt(scanner.nextLine());
        System.out.print("Введите логин: ");
        String login = scanner.nextLine();
        System.out.print("Введите пароль: ");
        String password = scanner.nextLine();

        String remotePath = "upload/addresses.json";

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(login, address, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();

            ChannelSftp channelSftp = (ChannelSftp) channel;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channelSftp.get(remotePath, outputStream);
            InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            HashMap<String, String> addresses = JsonReader.readJson(inputStream);
            System.out.print("Подключение установлено. ");
            while(true) {
                System.out.print("Меню возможных действий: \n" +
                        "1. Получение списка пар \"домен – адрес\" из файла.\n" +
                        "2. Получение IP-адреса по доменному имени.\n" +
                        "3. Получение доменного имени по IP-адресу.\n" +
                        "4. Добавление новой пары \"домен – адрес\" в файл.\n" +
                        "5. Удаление пары \"домен – адрес\" по доменному имени или IP-адресу.\n" +
                        "6. Завершение работы.\n" +
                        "Введите номер действия чтобы продолжить.\n");

                    int action = Integer.parseInt(scanner.nextLine());

                    switch (action) {
                        case 1:
                            listAddresses(addresses);
                            break;
                        case 2:
                            System.out.print("Введите доменное имя: ");
                            String domain = scanner.nextLine();
                            getIpAddress(addresses, domain);
                            break;
                        case 3:
                            System.out.print("Введите Ip-адрес: ");
                            String ip = scanner.nextLine();
                            getDomain(addresses, ip);
                            break;
                        case 4:
                            System.out.print("Введите новое доменное имя: ");
                            String newDomain = scanner.nextLine();
                            System.out.print("Введите новый IP-адрес: ");
                            String newIp = scanner.nextLine();
                            addNewAddress(addresses, newDomain, newIp);
                            break;
                        case 5:
                            System.out.print("Введите домен или IP-адрес для удаления: ");
                            String valueForDeletion = scanner.nextLine();
                            deleteAddress(addresses, valueForDeletion);
                            break;
                        case 6:
                            InputStream newInputStream = saveAddresses(addresses);
                            channelSftp.put(newInputStream, remotePath);
                            exit(channelSftp, session);
                            return;
                        default:
                            System.out.print("Некорректный выбор действия.");
                            break;
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static HashMap<String, String> listAddresses(HashMap<String, String> addresses) {
        TreeMap<String, String> sortedAddresses = new TreeMap<>(addresses);
        for (Map.Entry<String, String> entry : sortedAddresses.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        return addresses;
    }
    
    public static String getIpAddress(HashMap<String, String> addresses, String domain) {
        String ip = addresses.get(domain);
        if (ip != null) {
            System.out.println("IP-Адрес найден: " + ip);
        } else {
            System.out.println("Данного доменного имени не существует.");
        }
        return ip;
    }

    public static String getDomain(HashMap<String, String> addresses, String ip) {
        String domain = getDomainByIp(addresses, ip);
        if (domain != null) {
            System.out.println("Домен найден: " + domain);
        } else {
            System.out.println("Данного IP-адреса не существует.");
        }
        return domain;
    }

    public static String getDomainByIp(HashMap<String, String> addresses, String ip) {
        for (HashMap.Entry<String, String> entry : addresses.entrySet()) {
            if (entry.getValue().equals(ip)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static HashMap<String,String> addNewAddress(HashMap<String, String> addresses, String newDomain, String newIp) {
        if ((getIpAddress(addresses, newDomain) != null) || (getDomain(addresses, newIp) != null)) {
            System.out.println("Домен или IP-адрес уже есть в системе.");
            return addresses;
        }
        if(!isValidIPv4(newIp)) {
            System.out.println("Введён некорректный IP-адрес.");
            return addresses;
        }
        addresses.put(newDomain, newIp);
        System.out.println("Адрес успешно добавлен.");
        return addresses;
    }

    public static boolean isValidIPv4(String ip) {
        String[] parts = ip.split("\\.");
        
        if (parts.length != 4) {
            return false;
        }

        for (String part : parts) {
            if (!isNumeric(part)) {
                return false;
            }

            int number = Integer.parseInt(part);
            
            if (number < 0 || number > 255) {
                return false;
            }
            
            if (part.length() > 1 && part.startsWith("0")) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static HashMap<String, String> deleteAddress(HashMap<String, String> addresses, String valueForDeletion) {
        if (isValidIPv4(valueForDeletion)){
            addresses = deleteAddressByIp(addresses, valueForDeletion);
        }
        else {
            addresses = deleteAddressByDomain(addresses, valueForDeletion);
        }
        return addresses;
    }

    public static HashMap<String, String> deleteAddressByIp(HashMap<String, String> addresses, String ip) {
        String domain = getDomainByIp(addresses, ip);
        if(domain != null) {
            addresses.remove(domain);
            System.out.println("Адрес успешно удалён.");
            return addresses;
        }
        System.out.println("Данного IP-адреса не существует.");
        return addresses;
    }

    public static HashMap<String, String> deleteAddressByDomain(HashMap<String, String> addresses, String domain) {
        if(addresses.containsKey(domain)) {
            addresses.remove(domain);
            System.out.println("Адрес успешно удалён.");
            return addresses;
        }
        System.out.println("Данного домена не существует.");
        return addresses;
    }

    public static InputStream saveAddresses(HashMap<String, String> addresses) {
        String addressesFile = "{\n \t \"addresses\": [\n";

        Iterator<Map.Entry<String, String>> iterator = addresses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> address = iterator.next();
            String newAddress = "\t\t{\n\t\t\t\"domain\": \"" + address.getKey() + "\",\n\t\t\t\"ip\": \"" + address.getValue() + "\"\n\t\t}";
            if (!iterator.hasNext()) {
                newAddress += "\n";
            } else {
                newAddress += ",\n";
            }
            addressesFile += newAddress;
        }
        addressesFile += "\n\t]\n}";
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(addressesFile.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    public static void exit(ChannelSftp channelSftp, Session session) {
        channelSftp.exit();
        session.disconnect();
    }
}