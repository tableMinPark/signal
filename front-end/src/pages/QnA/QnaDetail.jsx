import React, { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import 'assets/styles/notice.css'
import AlertModal from 'components/AlertModal'
import view from 'assets/image/view.png'
import commentimg from 'assets/image/comment.png'
import EditIcon from '@mui/icons-material/Edit'
import trashcan from 'assets/image/TrashLetter.png'
import api from 'api/Api.js'

function QnaDetail() {
  const location = useLocation()
  const qnaSeq = parseInt(location.state.id)
  const [data, setData] = useState([])
  const [alertOpen, setAlertOpen] = useState(false)

  useEffect(() => {
    api.get(process.env.REACT_APP_API_URL + `/board/qna/` + qnaSeq).then((res) => {
      setData(res.data.body)
    })
  }, [])
  const navigate = useNavigate()

  const handleToTrash = () => {
    api.delete(process.env.REACT_APP_API_URL + `/board/qna/` + qnaSeq).then((res) => {
      setAlertOpen(true)
    })
  }

  const handleAlert = (e) => {
    setAlertOpen(false)
    navigate(`/qna`)
  }

  const handleToModify = () => {
    navigate(`/qnaModify`, { state: { qnaSeq: data.qnaSeq, title: data.title, content: data.content } })
  }

  return (
    <div className="qna-page-container">
      <div className="qna-detail-container">
        <div className="qna-detail-comment-container">
          <div className="qna-detail-header">
            <div className="qna-detail-title">
              <span className="qna-detail-qnanum">
                Q<span className="qna-detail-qnaSeq">{qnaSeq}</span>.
              </span>
              <span className="qna-detail-data-title"> {data.title}</span>
            </div>
            <div className="qna-detail-header-right">
              <EditIcon className="qna-detail-modify" onClick={handleToModify}></EditIcon>
              <img className="qna-detail-delete" src={trashcan} alt="trashcan" onClick={handleToTrash} />
              <AlertModal open={alertOpen} onClick={handleAlert} msg="삭제하시겠습니까?"></AlertModal>
            </div>
          </div>
          <div className="qna-detail-middle">
            <div className="qna-detail-middle-regDt">{data.regDt}</div>
            <div className="qna-detail-middle-view">
              <span>
                <img className="qna-detail-viewimg" src={view} alt="view" />
              </span>
              <span>{data.view}</span>
            </div>
          </div>
          <div className="qna-detail-content">
            {(data.content || '').split('\n').map((line, index) => {
              return (
                <span key={index}>
                  {line}
                  <br />
                </span>
              )
            })}
          </div>
        </div>
        {data.answer !== null ? (
          <div className="qna-detail-comment">
            <div className="qna-detail-comment-left">A</div>
            <div className="qna-detail-comment-right">
              <div className="qna-detail-comment-sb">
                <img src={commentimg} alt="" />
              </div>
              <div className="qna-detail-comment-answer">{data.answer}</div>
            </div>
          </div>
        ) : (
          <div></div>
        )}
      </div>
    </div>
  )
}

export default QnaDetail
