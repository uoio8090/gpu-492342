import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import exception.AppException;
import model.Member;
import model.Root;

/** 회원 데이터를 JSON 파일(data/members.json)로 읽고 쓰는 클래스. */
public class MemberStorage {

    static final Path DATA_FILE = Path.of("data", "members.json");
    static final Path TEMP_DATA_FILE = Path.of("data", "members.json.tmp");
    static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 데이터 파일에서 회원 목록을 읽어 반환한다. 파일이 없으면 빈 목록을 반환한다. */
    static List<Member> load() throws IOException {
        if (!Files.exists(DATA_FILE)) {
            return new ArrayList<>();
        }

        String jsonText = Files.readString(DATA_FILE);

        if (jsonText.isBlank()) {
            throw new IOException("데이터 파일이 비어 있습니다.");
        }

        Root root = OBJECT_MAPPER.readValue(jsonText, Root.class);

        if (root == null || root.members == null) {
            throw new IOException("데이터 파일에 members 목록이 없습니다.");
        }

        return root.members;
    }

    /** 회원 목록을 파일에 저장한다. 임시 파일에 먼저 쓴 뒤 교체해 파일 손상을 막는다. */
    static void save(Collection<Member> members) {
        try {
            Files.createDirectories(Path.of("data"));

            Root root = new Root();
            root.members = new ArrayList<>(members);
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(TEMP_DATA_FILE.toFile(), root);

            try {
                Files.move(TEMP_DATA_FILE, DATA_FILE, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(TEMP_DATA_FILE, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new AppException("저장 실패: " + e.getMessage());
        }
    }
}
