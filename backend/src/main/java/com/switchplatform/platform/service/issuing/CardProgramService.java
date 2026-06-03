package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.CardProduct;
import com.switchplatform.platform.model.issuing.CardProgram;
import com.switchplatform.platform.repository.issuing.CardProductRepository;
import com.switchplatform.platform.repository.issuing.CardProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardProgramService {

    private final CardProgramRepository programRepository;
    private final CardProductRepository productRepository;

    @Transactional
    public CardProgram createProgram(CardProgram program) {
        return programRepository.save(program);
    }

    @Transactional
    public CardProgram updateProgram(UUID id, CardProgram update) {
        CardProgram existing = programRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + id));
        existing.setName(update.getName());
        existing.setDescription(update.getDescription());
        existing.setProgramType(update.getProgramType());
        existing.setBrand(update.getBrand());
        existing.setStartDate(update.getStartDate());
        existing.setEndDate(update.getEndDate());
        existing.setMetadata(update.getMetadata());
        return programRepository.save(existing);
    }

    @Transactional
    public CardProgram activateProgram(UUID id) {
        CardProgram p = programRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + id));
        p.setStatus(CardProgram.Status.ACTIVE);
        return programRepository.save(p);
    }

    @Transactional
    public CardProgram deactivateProgram(UUID id) {
        CardProgram p = programRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + id));
        p.setStatus(CardProgram.Status.INACTIVE);
        return programRepository.save(p);
    }

    @Transactional
    public void deleteProgram(UUID id) {
        List<CardProduct> products = productRepository.findByProgramId(id);
        productRepository.deleteAll(products);
        programRepository.deleteById(id);
    }

    public CardProgram getProgram(UUID id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Program not found: " + id));
    }

    public List<CardProgram> listPrograms() {
        return programRepository.findAll();
    }

    @Transactional
    public CardProduct createProduct(CardProduct product) {
        return productRepository.save(product);
    }

    @Transactional
    public CardProduct updateProduct(UUID id, CardProduct update) {
        CardProduct existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        existing.setName(update.getName());
        existing.setDescription(update.getDescription());
        existing.setCardType(update.getCardType());
        existing.setCardBrand(update.getCardBrand());
        existing.setCardNetwork(update.getCardNetwork());
        existing.setContactlessEnabled(update.getContactlessEnabled());
        existing.setOnlineEnabled(update.getOnlineEnabled());
        existing.setInternationalEnabled(update.getInternationalEnabled());
        existing.setEcommerceEnabled(update.getEcommerceEnabled());
        existing.setAtmEnabled(update.getAtmEnabled());
        existing.setMagStripeEnabled(update.getMagStripeEnabled());
        existing.setChipEnabled(update.getChipEnabled());
        existing.setIsRenewable(update.getIsRenewable());
        existing.setIsReissuable(update.getIsReissuable());
        existing.setIsVirtualSupported(update.getIsVirtualSupported());
        existing.setDailyLimit(update.getDailyLimit());
        existing.setWeeklyLimit(update.getWeeklyLimit());
        existing.setMonthlyLimit(update.getMonthlyLimit());
        existing.setSingleTxnLimit(update.getSingleTxnLimit());
        existing.setAnnualFee(update.getAnnualFee());
        existing.setCurrencyCode(update.getCurrencyCode());
        existing.setFeatures(update.getFeatures());
        existing.setMetadata(update.getMetadata());
        return productRepository.save(existing);
    }

    @Transactional
    public CardProduct activateProduct(UUID id) {
        CardProduct p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        p.setStatus(CardProduct.Status.ACTIVE);
        return productRepository.save(p);
    }

    @Transactional
    public CardProduct deactivateProduct(UUID id) {
        CardProduct p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        p.setStatus(CardProduct.Status.INACTIVE);
        return productRepository.save(p);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id);
    }

    public CardProduct getProduct(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    public CardProduct getProductByCode(String code) {
        return productRepository.findByProductCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + code));
    }

    public List<CardProduct> getProductsByProgram(UUID programId) {
        return productRepository.findByProgramId(programId);
    }

    public List<CardProduct> listProducts() {
        return productRepository.findAll();
    }
}
