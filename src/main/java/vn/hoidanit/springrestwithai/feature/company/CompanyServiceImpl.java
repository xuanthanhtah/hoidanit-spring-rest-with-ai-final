package vn.hoidanit.springrestwithai.feature.company;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.springrestwithai.dto.PageResponse;
import vn.hoidanit.springrestwithai.exception.DuplicateResourceException;
import vn.hoidanit.springrestwithai.exception.ResourceNotFoundException;
import vn.hoidanit.springrestwithai.feature.company.dto.CompanyResponse;
import vn.hoidanit.springrestwithai.feature.company.dto.CreateCompanyRequest;
import vn.hoidanit.springrestwithai.feature.company.dto.UpdateCompanyRequest;

@Service
public class CompanyServiceImpl implements CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyServiceImpl.class);
    private static final String RESOURCE_NAME = "Công ty";

    private final CompanyRepository companyRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public PageResponse<CompanyResponse> getAllCompanies(Pageable pageable) {
        Page<Company> page = this.companyRepository.findAll(pageable);
        return PageResponse.of(page, page.map(CompanyResponse::fromEntity).getContent());
    }

    @Override
    public CompanyResponse getCompanyById(Long id) {
        return CompanyResponse.fromEntity(findCompanyOrThrow(id));
    }

    @Override
    @Transactional
    public CompanyResponse createCompany(CreateCompanyRequest request) {
        if (this.companyRepository.existsByName(request.name())) {
            throw duplicateException(request.name());
        }
        Company company = new Company();
        company.setName(request.name());
        company.setDescription(request.description());
        company.setAddress(request.address());
        company.setLogo(request.logo());
        Company saved = this.companyRepository.save(company);
        log.info("Created company id={}", saved.getId());
        return CompanyResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CompanyResponse updateCompany(UpdateCompanyRequest request) {
        Company company = findCompanyOrThrow(request.id());
        if (this.companyRepository.existsByNameAndIdNot(request.name(), request.id())) {
            throw duplicateException(request.name());
        }
        company.setName(request.name());
        company.setDescription(request.description());
        company.setAddress(request.address());
        company.setLogo(request.logo());
        Company saved = this.companyRepository.save(company);
        log.info("Updated company id={}", saved.getId());
        return CompanyResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteCompany(Long id) {
        Company company = findCompanyOrThrow(id);
        // Company is the inverse side: users survive the delete with company_id = NULL
        company.getUsers().forEach(user -> user.setCompany(null));
        company.getUsers().clear();
        this.companyRepository.delete(company);
        log.info("Deleted company id={}", id);
    }

    private Company findCompanyOrThrow(Long id) {
        return this.companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE_NAME, "id", id));
    }

    private DuplicateResourceException duplicateException(String name) {
        return new DuplicateResourceException(RESOURCE_NAME, "name", name);
    }
}
