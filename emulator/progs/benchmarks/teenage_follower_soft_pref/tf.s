	.file	"tf.cpp"
	.option nopic
	.attribute arch, "rv64i2p0_m2p0_a2p0_f2p0_d2p0_c2p0"
	.attribute unaligned_access, 0
	.attribute stack_align, 16
	.text
	.section	.rodata.str1.8,"aMS",@progbits,1
	.align	3
.LC0:
	.string	"Read graph %s\n"
	.align	3
.LC1:
	.string	"r"
	.align	3
.LC2:
	.string	"%d"
	.align	3
.LC3:
	.string	"Rows = %d Non-zero = %d \n"
	.align	3
.LC4:
	.string	"%f"
	.text
	.align	1
	.globl	_Z10read_graphPc
	.type	_Z10read_graphPc, @function
_Z10read_graphPc:
.LFB1162:
	.cfi_startproc
	addi	sp,sp,-112
	.cfi_def_cfa_offset 112
	sd	s5,56(sp)
	.cfi_offset 21, -56
	mv	s5,a0
	lui	a0,%hi(.LC0)
	addi	a0,a0,%lo(.LC0)
	sd	ra,104(sp)
	sd	s0,96(sp)
	sd	s1,88(sp)
	.cfi_offset 1, -8
	.cfi_offset 8, -16
	.cfi_offset 9, -24
	mv	s0,a1
	sd	s2,80(sp)
	sd	s3,72(sp)
	sd	s4,64(sp)
	sd	s6,48(sp)
	sd	s7,40(sp)
	.cfi_offset 18, -32
	.cfi_offset 19, -40
	.cfi_offset 20, -48
	.cfi_offset 22, -64
	.cfi_offset 23, -72
	call	printf
	lui	a1,%hi(.LC1)
	addi	a1,a1,%lo(.LC1)
	mv	a0,s0
	call	fopen
	lui	s6,%hi(.LC2)
	addi	a2,sp,12
	addi	a1,s6,%lo(.LC2)
	mv	s3,a0
	call	fscanf
	addi	a2,sp,16
	addi	a1,s6,%lo(.LC2)
	mv	a0,s3
	call	fscanf
	lw	a2,16(sp)
	lw	a1,12(sp)
	lui	a0,%hi(.LC3)
	addi	a0,a0,%lo(.LC3)
	call	printf
	lw	s4,12(sp)
	lw	s7,16(sp)
	addiw	a0,s4,1
	slli	a0,a0,2
	slli	s2,s7,2
	sw	s4,28(s5)
	sw	s7,32(s5)
	call	malloc
	mv	s0,a0
	mv	a0,s2
	sd	s0,0(s5)
	call	malloc
	mv	s1,a0
	mv	a0,s2
	sd	s1,8(s5)
	call	malloc
	sd	a0,16(s5)
	sw	s4,24(s5)
	mv	s2,a0
	blt	s4,zero,.L2
	li	s4,0
.L3:
	addi	a2,sp,20
	addi	a1,s6,%lo(.LC2)
	mv	a0,s3
	call	fscanf
	lw	a4,20(sp)
	lw	a5,12(sp)
	addiw	s4,s4,1
	sw	a4,0(s0)
	addi	s0,s0,4
	bge	a5,s4,.L3
	lw	s7,16(sp)
.L2:
	mv	s0,s1
	li	s1,0
	ble	s7,zero,.L1
.L5:
	addi	a2,sp,24
	addi	a1,s6,%lo(.LC2)
	mv	a0,s3
	call	fscanf
	lw	a4,24(sp)
	lw	a5,16(sp)
	addiw	s1,s1,1
	sw	a4,0(s0)
	addi	s0,s0,4
	bgt	a5,s1,.L5
	ble	a5,zero,.L1
	mv	s0,s2
	li	s1,0
	lui	s2,%hi(.LC4)
.L7:
	addi	a2,sp,28
	addi	a1,s2,%lo(.LC4)
	mv	a0,s3
	call	fscanf
	flw	fa5,28(sp)
	lw	a5,16(sp)
	addi	s0,s0,4
	fcvt.w.s a4,fa5,rtz
	addiw	s1,s1,1
	sw	a4,-4(s0)
	bgt	a5,s1,.L7
.L1:
	ld	ra,104(sp)
	.cfi_restore 1
	ld	s0,96(sp)
	.cfi_restore 8
	ld	s1,88(sp)
	.cfi_restore 9
	ld	s2,80(sp)
	.cfi_restore 18
	ld	s3,72(sp)
	.cfi_restore 19
	ld	s4,64(sp)
	.cfi_restore 20
	ld	s6,48(sp)
	.cfi_restore 22
	ld	s7,40(sp)
	.cfi_restore 23
	mv	a0,s5
	ld	s5,56(sp)
	.cfi_restore 21
	addi	sp,sp,112
	.cfi_def_cfa_offset 0
	jr	ra
	.cfi_endproc
.LFE1162:
	.size	_Z10read_graphPc, .-_Z10read_graphPc
	.section	.rodata.str1.8
	.align	3
.LC5:
	.string	"X=%d \n"
	.text
	.align	1
	.globl	_Z9benchmarkPiS_
	.type	_Z9benchmarkPiS_, @function
_Z9benchmarkPiS_:
.LFB1163:
	.cfi_startproc
	addi	sp,sp,-80
	.cfi_def_cfa_offset 80
	sd	s0,64(sp)
	.cfi_offset 8, -16
	lui	s0,%hi(.LANCHOR0)
	addi	s0,s0,%lo(.LANCHOR0)
	lw	a5,28(s0)
	sd	s1,56(sp)
	sd	s2,48(sp)
	sd	s3,40(sp)
	sd	s4,32(sp)
	sd	s5,24(sp)
	sd	s6,16(sp)
	sd	s7,8(sp)
	sd	ra,72(sp)
	sd	s8,0(sp)
	.cfi_offset 9, -24
	.cfi_offset 18, -32
	.cfi_offset 19, -40
	.cfi_offset 20, -48
	.cfi_offset 21, -56
	.cfi_offset 22, -64
	.cfi_offset 23, -72
	.cfi_offset 1, -8
	.cfi_offset 24, -80
	mv	s2,a0
	mv	s6,a1
	li	s7,0
	li	s4,0
	li	s3,1
	lui	s1,%hi(stride)
	li	s5,10
.L15:
	beq	a5,zero,.L22
	li	s8,0
.L16:
	call	rand
	srliw	a4,a0,31
	slli	a3,s8,32
	addw	a0,a4,a0
	srli	a5,a3,30
	andi	a0,a0,1
	add	a3,s2,a5
	subw	a0,a0,a4
	sw	a0,0(a3)
	add	a5,s6,a5
	sw	zero,0(a5)
	lw	a5,28(s0)
	addiw	s8,s8,1
	bgtu	a5,s8,.L16
	beq	a5,zero,.L22
	ld	a1,0(s0)
	ld	t3,8(s0)
	mv	t1,s2
	addi	a1,a1,4
	li	a7,0
	j	.L20
.L18:
	addi	t1,t1,4
	addi	a1,a1,4
	bleu	a5,a7,.L32
.L20:
	lw	a4,0(t1)
	addi	a7,a7,1
	bne	a4,s3,.L18
	lw	a2,-4(a1)
	lw	a4,0(a1)
	bgeu	a2,a4,.L18
	slli	a0,a2,2
	add	a0,t3,a0
.L19:
	lw	a6,0(a0)
	lw	a5,%lo(stride)(s1)
	addi	a2,a2,1
	slli	a4,a6,2
	add	a4,s6,a4
	lw	a3,0(a4)
	addw	a5,a5,a6
	slli	a5,a5,2
	add	a5,s6,a5
	addiw	a3,a3,1
	lw	a6,0(a5)
	sw	a3,0(a4)
	lw	a5,0(a1)
	addw	s7,a6,s7
	addi	a0,a0,4
	bgtu	a5,a2,.L19
	lw	a5,28(s0)
	addi	t1,t1,4
	addi	a1,a1,4
	bgtu	a5,a7,.L20
.L32:
	addiw	s4,s4,1
	bne	s4,s5,.L15
.L22:
	ld	s0,64(sp)
	.cfi_restore 8
	ld	ra,72(sp)
	.cfi_restore 1
	ld	s1,56(sp)
	.cfi_restore 9
	ld	s2,48(sp)
	.cfi_restore 18
	ld	s3,40(sp)
	.cfi_restore 19
	ld	s4,32(sp)
	.cfi_restore 20
	ld	s5,24(sp)
	.cfi_restore 21
	ld	s6,16(sp)
	.cfi_restore 22
	ld	s8,0(sp)
	.cfi_restore 24
	mv	a1,s7
	ld	s7,8(sp)
	.cfi_restore 23
	lui	a0,%hi(.LC5)
	addi	a0,a0,%lo(.LC5)
	addi	sp,sp,80
	.cfi_def_cfa_offset 0
	tail	printf
	.cfi_endproc
.LFE1163:
	.size	_Z9benchmarkPiS_, .-_Z9benchmarkPiS_
	.section	.rodata.str1.8
	.align	3
.LC6:
	.string	"Teenage Follower Finished"
	.section	.text.startup,"ax",@progbits
	.align	1
	.globl	main
	.type	main, @function
main:
.LFB1164:
	.cfi_startproc
	.cfi_personality 0x9b,DW.ref.__gxx_personality_v0
	.cfi_lsda 0x1b,.LLSDA1164
	addi	sp,sp,-528
	.cfi_def_cfa_offset 528
	sd	s1,504(sp)
	li	a0,9
	.cfi_offset 9, -24
	mv	s1,a1
	li	a1,0
	sd	ra,520(sp)
	sd	s0,512(sp)
	sd	s2,496(sp)
	.cfi_offset 1, -8
	.cfi_offset 8, -16
	.cfi_offset 18, -32
.LEHB0:
	call	atom_init
	ld	a1,8(s1)
	mv	a0,sp
	lui	s0,%hi(.LANCHOR0)
	call	_Z10read_graphPc
	ld	a5,0(sp)
	addi	s0,s0,%lo(.LANCHOR0)
	ld	a0,16(s1)
	sd	a5,0(s0)
	ld	a5,8(sp)
	sd	a5,8(s0)
	ld	a5,16(sp)
	sd	a5,16(s0)
	ld	a5,24(sp)
	sd	a5,24(s0)
	ld	a5,32(sp)
	sd	a5,32(s0)
	call	atoi
	lw	s2,28(s0)
	mv	a4,a0
	lui	a5,%hi(stride)
	slli	s2,s2,2
	li	a1,1
	mv	a0,s2
	sw	a4,%lo(stride)(a5)
	call	calloc
	li	a1,1
	mv	s1,a0
	mv	a0,s2
	call	calloc
	mv	s2,a0
	li	a0,1337
	call	srand
	addi	a0,sp,56
	call	_ZN3HPCC1Ev
.LEHE0:
	addi	a0,sp,56
.LEHB1:
	call	_ZN3HPC16startMeasurementEv
	lw	a5,28(s0)
	slli	a5,a5,2
	srli	a5,a5,9
	addiw	a5,a5,1
	slli	a5,a5,32
	srli	a5,a5,32
 #APP
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
 #NO_APP
	li	a3,0
	ld	a4,0(s0)
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map a4, a3 
	
# 0 "" 2
 #NO_APP
	lw	a4,32(s0)
	slli	a4,a4,2
	srli	a4,a4,9
	addiw	a4,a4,1
	slli	a4,a4,32
	srli	a4,a4,32
 #APP
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a4
	
# 0 "" 2
 #NO_APP
	ld	a2,8(s0)
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map a2, a3 
	
# 0 "" 2
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a4
	
# 0 "" 2
 #NO_APP
	ld	a4,16(s0)
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map a4, a3 
	
# 0 "" 2
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
# 71 "../../XMemLib/atomops.h" 1
	amu_map s1, a3 
	
# 0 "" 2
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
# 71 "../../XMemLib/atomops.h" 1
	amu_map s2, a3 
	
# 0 "" 2
 #NO_APP
	mv	a1,s2
	mv	a0,s1
	call	_Z9benchmarkPiS_
	addi	a0,sp,56
	call	_ZN3HPC14endMeasurementEv
	lui	a0,%hi(.LC6)
	addi	a0,a0,%lo(.LC6)
	call	puts
	addi	a0,sp,56
	call	_ZN3HPC10printStatsEv
	addi	a0,sp,56
	call	_ZN3HPC8printCSVEv
.LEHE1:
	addi	a0,sp,56
	call	_ZN3HPCD1Ev
	ld	ra,520(sp)
	.cfi_remember_state
	.cfi_restore 1
	ld	s0,512(sp)
	.cfi_restore 8
	ld	s1,504(sp)
	.cfi_restore 9
	ld	s2,496(sp)
	.cfi_restore 18
	li	a0,0
	addi	sp,sp,528
	.cfi_def_cfa_offset 0
	jr	ra
.L35:
	.cfi_restore_state
	mv	s0,a0
	addi	a0,sp,56
	call	_ZN3HPCD1Ev
	mv	a0,s0
.LEHB2:
	call	_Unwind_Resume
.LEHE2:
	.cfi_endproc
.LFE1164:
	.globl	__gxx_personality_v0
	.section	.gcc_except_table,"aw",@progbits
.LLSDA1164:
	.byte	0xff
	.byte	0xff
	.byte	0x3
	.byte	0x27
	.4byte	.LEHB0-.LFB1164
	.4byte	.LEHE0-.LEHB0
	.4byte	0
	.byte	0
	.4byte	.LEHB1-.LFB1164
	.4byte	.LEHE1-.LEHB1
	.4byte	.L35-.LFB1164
	.byte	0
	.4byte	.LEHB2-.LFB1164
	.4byte	.LEHE2-.LEHB2
	.4byte	0
	.byte	0
	.section	.text.startup
	.size	main, .-main
	.globl	stride
	.globl	csr_graph
	.bss
	.align	3
	.set	.LANCHOR0,. + 0
	.type	csr_graph, @object
	.size	csr_graph, 40
csr_graph:
	.zero	40
	.section	.sbss,"aw",@nobits
	.align	2
	.type	stride, @object
	.size	stride, 4
stride:
	.zero	4
	.hidden	DW.ref.__gxx_personality_v0
	.weak	DW.ref.__gxx_personality_v0
	.section	.sdata.DW.ref.__gxx_personality_v0,"awG",@progbits,DW.ref.__gxx_personality_v0,comdat
	.align	3
	.type	DW.ref.__gxx_personality_v0, @object
	.size	DW.ref.__gxx_personality_v0, 8
DW.ref.__gxx_personality_v0:
	.dword	__gxx_personality_v0
	.ident	"GCC: (g2ee5e430018) 12.2.0"
